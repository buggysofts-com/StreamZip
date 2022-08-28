package com.buggysofts.streamzip;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public abstract class ChunkProperties {
    protected long offset;
    protected int size;
}
