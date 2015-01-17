package com.lemonsqueeze.fakewificonnection;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PatternMatcher;
import java.util.*;


public class HookManager
{
    public class Hook
    {
	public BroadcastReceiver receiver;
	public Context context;
	public IntentFilter filter;

	public Hook(IntentFilter f, BroadcastReceiver r, Context c)
	{
	    receiver = r;
	    context = c;
	    filter = f;
	}
    }

    public interface IntentGenCallback
    {
	public void handle(Intent intent);
    }

    // map receiver to List<hook> for that receiver
    private HashMap<BroadcastReceiver, List<Hook>>  hash = new HashMap();

        
    public void add(IntentFilter filter, BroadcastReceiver receiver, Context context)
    {
	List<Hook> l = hash.get(receiver);
	if (l == null)
	{
	    l = new ArrayList<Hook>();
	    hash.put(receiver, l);
	}
	
	l.add(new Hook(filter, receiver, context));
    }

    public void remove(BroadcastReceiver receiver)
    {
	hash.remove(receiver);
    }

    private Intent make_intent(String action)
    {
	Intent intent = new Intent(action);
	IntentGenCallback f = intent_gens.get(action);
	if (f != null)
	    f.handle(intent);
	return intent;
    }
    
    public void call(String action)
    {
	Iterator i = hash.entrySet().iterator();
	Intent intent = make_intent(action);
	
	while (i != null && i.hasNext())
	{
	    Map.Entry entry = (Map.Entry) i.next();
	    List<Hook> list = (List<Hook>) entry.getValue();
	    
	    for (Hook h: list)
		if (h.filter.hasAction(action))
		    h.receiver.onReceive(h.context, intent);
	}
    }

    /********************************************************************/

    // hooked actions
    private Set<String>  hooked_actions = new HashSet();
    private HashMap<String, IntentGenCallback>  intent_gens = new HashMap();
    
    // keep track of receivers we let through
    private HashMap<BroadcastReceiver, Integer>  outside_receivers = new HashMap();

    public void add_action(String action, IntentGenCallback f)
    {
	hooked_actions.add(action);
	intent_gens.put(action, f);
    }

    private int get_outside_receivers(BroadcastReceiver r)
    {
	Integer i = outside_receivers.get(r);
	return (i != null ? i : 0);
    }
    
    private void add_outside_receiver(BroadcastReceiver r)
    {
	int i = get_outside_receivers(r);
	outside_receivers.put(r, i + 1);
    }

    private void remove_outside_receivers(BroadcastReceiver r)
    {
	outside_receivers.remove(r);
    }

    // make intent with the first hooked action (...)
    public Intent make_returned_intent(final IntentFilter filter)
    {	
	Iterator<String> i = filter.actionsIterator();
	while (i != null && i.hasNext())
	{
	    String action = i.next();
	    if (hooked_actions.contains(action))
		return make_intent(action);
	}
	return null;
    }
	    
    // create a copy of filter with just the unhooked actions
    public IntentFilter split_intent_filter(final IntentFilter filter) throws Exception
    {
	IntentFilter f = new IntentFilter();
	
	{ Iterator<String> i = filter.actionsIterator();
	    while (i != null && i.hasNext())
	    {
		String action = i.next();
		if (!hooked_actions.contains(action))
		    f.addAction(action);
	    }
	}
	
	{ Iterator<String> i = filter.categoriesIterator();  // gosh this is ugly
	    while (i != null && i.hasNext())
		f.addCategory(i.next());
	}

	{ Iterator<IntentFilter.AuthorityEntry> i = filter.authoritiesIterator();
	    while (i != null && i.hasNext())
	    {
		IntentFilter.AuthorityEntry entry = i.next();
		String port = (entry.getPort() != -1 ? Integer.valueOf(entry.getPort()).toString() : null);
		f.addDataAuthority(entry.getHost(), port);
	    }
	}

	{ Iterator<PatternMatcher> i = filter.pathsIterator();
	    while (i != null && i.hasNext())
	    {
		PatternMatcher p = i.next();
		f.addDataPath(p.getPath(), p.getType());
	    }
	}

	{ Iterator<String> i = filter.schemesIterator();
	    while (i != null && i.hasNext())
		f.addDataScheme(i.next());
	}


	try	// schemeSpecificPartsIterator() -> API 19
	{
	    // Iterator<PatternMatcher> i = filter.schemeSpecificPartsIterator();
	    Iterator<PatternMatcher> i = (Iterator<PatternMatcher>)
	      IntentFilter.class.getMethod("schemeSpecificPartsIterator", null)
	        .invoke(filter, new Object[0]);

	    while (i != null && i.hasNext())
	    {
		PatternMatcher p = i.next();
		// f.addDataSchemeSpecificPart(p.getPath(), p.getType());
		IntentFilter.class.getMethod("addDataSchemeSpecificPart",
					     new Class[]{String.class, Integer.class})
		  .invoke(f, p.getPath(), p.getType());
	    }
	}
	catch (Exception e) {}

	{ Iterator<String> i = filter.typesIterator();
	    while (i != null && i.hasNext())
		f.addDataType(i.next());
	}
	
	return f;
    }
    
    public void handle_register(MethodHookParam param) throws Throwable
    {
	BroadcastReceiver receiver = (BroadcastReceiver) param.args[0];
	IntentFilter filter = (IntentFilter) param.args[1];
	
	int nactions = filter.countActions();
	boolean has_hooked = false;
	boolean has_unhooked = false;
	
	Iterator<String> i = filter.actionsIterator();
	while (i != null && i.hasNext())
	    if (hooked_actions.contains(i.next()))
		has_hooked = true;
	    else
		has_unhooked = true;

	if (nactions == 0 || has_unhooked)  // no actions is wildcard
	    add_outside_receiver(receiver);
	
	if (!has_hooked)	// let it through
	    return;
	
	this.add(filter, receiver, (Context) param.thisObject);	
	//log_call("registerReceiver(SCAN_RESULTS) called");	
	
	if (!has_unhooked)	// only hooked actions, 
	{
	    // param.args[0] = null;   // set receiver to null, we just want return value from orig call
	    param.setResult(make_returned_intent(filter));  // cancel orig call  (and set return)
	    return;
	}
	
	// some hooked and some unhooked actions, split it.
	IntentFilter unhooked_filter = split_intent_filter(filter);

	// FIXME hopefully that won't change return value in case it matters ...	
	param.args[1] = unhooked_filter;
	
    }
    
    public void handle_unregister(MethodHookParam param) throws Throwable
    {
	BroadcastReceiver receiver = (BroadcastReceiver) param.args[0];
	
	this.remove(receiver);
	
	if (get_outside_receivers(receiver) == 0)
	    param.setResult(null);	//cancel call
	remove_outside_receivers(receiver);
    }
    
}

