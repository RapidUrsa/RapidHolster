package com.rapidursa.holster;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class RapidHolsterPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(RapidHolsterPlugin.class);
        RuneLite.main(args);
    }
}
