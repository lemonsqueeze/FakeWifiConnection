package com.lemonsqueeze.fakewificonnection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

    // TODO if we expect many hooks use hash table instead
    private List<Hook> list = new ArrayList<Hook>();
   
    public void add(IntentFilter filter, BroadcastReceiver receiver, Context context)
    {
	list.add(new Hook(filter, receiver, context));
    }

    public void remove(BroadcastReceiver receiver)
    {
	Iterator<Hook> i = list.iterator();
	while (i.hasNext())
	{
	    Hook h = i.next();
	    if (h.receiver == receiver)
		i.remove();
	}
    }

    public void call(String action)
    {
	for (Hook h: list)
	    if (h.filter.hasAction(action))
		h.receiver.onReceive(h.context, new Intent(action));
    }
}

