/*
 * Decompiled with CFR 0_118.
 */
package Exceptions;

import Exceptions.ModbusException;

public class CRCCheckFailedException
extends ModbusException {
    public CRCCheckFailedException() {
    }

    public CRCCheckFailedException(String s) {
        super(s);
    }
}

