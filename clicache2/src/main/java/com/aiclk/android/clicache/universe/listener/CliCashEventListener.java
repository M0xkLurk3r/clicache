package com.aiclk.android.clicache.universe.listener;


import com.aiclk.android.clicache.universe.CliCash;

/**
 * Created by anthony on 5/5/18.
 */

public interface CliCashEventListener {
	void onCliCashBeginListening(CliCash cliCash);
	void onCliCashDestoryed(CliCash cliCash);
	void onCliCashCaughtError(CliCash cliCash, Exception exception);
}
