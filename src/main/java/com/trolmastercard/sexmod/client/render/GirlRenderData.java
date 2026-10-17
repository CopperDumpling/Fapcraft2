package com.trolmastercard.sexmod.client.render;

import com.geckolib.constant.dataticket.DataTicket;

/** Per-frame values copied from a girl into her render state (GeckoLib data tickets). */
public final class GirlRenderData {
    /** Dressed or nude model (1.12.2 {@code OUTFIT_INDEX}: 1 = dressed). */
    public static final DataTicket<Boolean> DRESSED = DataTicket.create("sexmod_dressed", Boolean.class);

    private GirlRenderData() {
    }
}
