/*************************************************************************************
 * Copyright (C) 2014-2020 GENERAL BYTES s.r.o. All rights reserved.
 *
 * This software may be distributed and modified under the terms of the GNU
 * General Public License version 2 (GPL2) as published by the Free Software
 * Foundation and appearing in the file GPL2.TXT included in the packaging of
 * this file. Please note that GPL2 Section 2[b] requires that all works based
 * on this software must also be made publicly available under the terms of
 * the GPL2 ("Copyleft").
 *
 * Contact information
 * -------------------
 *
 * GENERAL BYTES s.r.o.
 * Web      :  http://www.generalbytes.com
 *
 ************************************************************************************/
package com.generalbytes.batm.server.extensions.extra.blackcoin;

import com.generalbytes.batm.common.currencies.CryptoCurrency;
import com.generalbytes.batm.common.currencies.FiatCurrency;
import com.generalbytes.batm.server.extensions.*;
import com.generalbytes.batm.server.extensions.FixPriceRateSource;
import com.generalbytes.batm.server.extensions.extra.blackcoin.wallets.blackmored.BlackmoredRPCWallet;
import com.generalbytes.batm.server.extensions.extra.blackcoin.wallets.blackmored.BlackmoredUniqueAddressRPCWallet;
import com.generalbytes.batm.server.extensions.ExtensionsUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.*;

public class BlackcoinExtension extends AbstractExtension{
    private static final Logger log = LoggerFactory.getLogger(BlackcoinExtension.class);

    @Override
    public String getName() {
        return "BATM Blackcoin extension";
    }

    @Override
    public IWallet createWallet(String walletLogin, String tunnelPassword) {
        try {
        if (walletLogin !=null && !walletLogin.trim().isEmpty()) {
            StringTokenizer st = new StringTokenizer(walletLogin,":");
            String walletType = st.nextToken();

            if ("blackmored".equalsIgnoreCase(walletType)
                || "blackmorednoforward".equalsIgnoreCase(walletType)) {
                //"blackmored:protocol:user:password:ip:port:accountname"

                String protocol = st.nextToken();
                String username = st.nextToken();
                String password = st.nextToken();
                String hostname = st.nextToken();
                int port = Integer.parseInt(st.nextToken());
                String label = "";
                if (st.hasMoreTokens()) {
                    label = st.nextToken();
                }

                InetSocketAddress tunnelAddress = ctx.getTunnelManager().connectIfNeeded(walletLogin, tunnelPassword, InetSocketAddress.createUnresolved(hostname, port));
                hostname = tunnelAddress.getHostString();
                port = tunnelAddress.getPort();

                if (protocol != null && username != null && password != null && hostname !=null && label != null) {
                    String rpcURL = protocol +"://" + username +":" + password + "@" + hostname +":" + port;
                    if ("blackmorednoforward".equalsIgnoreCase(walletType)) {
                        return new BlackmoredUniqueAddressRPCWallet(rpcURL);
                    }
                    return new BlackmoredRPCWallet(rpcURL, label);
                }
            }
        }
        } catch (Exception e) {
            log.warn("createWallet failed for prefix: {}, {}: {} ",
                ExtensionsUtil.getPrefixWithCountOfParameters(walletLogin), e.getClass().getSimpleName(), e.getMessage()
            );
        }
        return null;
    }

    @Override
    public ICryptoAddressValidator createAddressValidator(String cryptoCurrency) {
        if (CryptoCurrency.BLK.getCode().equalsIgnoreCase(cryptoCurrency)) {
            return new BlackcoinAddressValidator();
        }
        return null;
    }

    @Override
    public IRateSource createRateSource(String sourceLogin) {
        if (sourceLogin != null && !sourceLogin.trim().isEmpty()) {
            try {
                StringTokenizer st = new StringTokenizer(sourceLogin, ":");
                String exchangeType = st.nextToken();

                if ("blkfix".equalsIgnoreCase(exchangeType)) {
                    BigDecimal rate = BigDecimal.ZERO;
                    if (st.hasMoreTokens()) {
                        try {
                            rate = new BigDecimal(st.nextToken());
                        } catch (Throwable e) {
                        }
                    }
                    String preferedFiatCurrency = FiatCurrency.USD.getCode();
                    if (st.hasMoreTokens()) {
                        preferedFiatCurrency = st.nextToken().toUpperCase();
                    }
                    return new FixPriceRateSource(rate, preferedFiatCurrency);
                }
            } catch (Exception e) {
                log.warn("createRateSource failed for prefix: {}, {}: {} ",
                    ExtensionsUtil.getPrefixWithCountOfParameters(sourceLogin), e.getClass().getSimpleName(), e.getMessage()
                );
            }
        }
        return null;
    }
    @Override
    public Set<String> getSupportedCryptoCurrencies() {
        Set<String> result = new HashSet<String>();
        result.add(CryptoCurrency.BLK.getCode());
        return result;
    }
}
