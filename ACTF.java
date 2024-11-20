package gr.aueb.delorean.chimp;

public class ACTF {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private long storedVal = 0;
    private boolean first = true;
    private int size;
    private long lastXorValue = 0;
    private int significantBits = 0;
    private int v = 0;
    public final static int THRESHOLD = 6;
    
    public final static short[] leadingRepresentation = {0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 1, 1, 1,
            2, 2, 2, 3, 3, 3, 4, 4,
            4, 5, 5, 6, 6, 6, 6, 7,
            7, 7, 7, 7, 7, 7, 7, 7,
            7, 7, 7, 7, 7, 7, 7, 7,
            7, 7, 7, 7, 7, 7, 7, 7
    };
    public final static short[] leadingRound = {0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 21, 21, 21,
            24, 24, 24, 27, 27, 27, 30, 30,
            30, 33, 33, 35, 35, 35, 35, 39,
            39, 39, 39, 39, 39, 39, 39, 39,
            39, 39, 39, 39, 39, 39, 39, 39,
            39, 39, 39, 39, 39, 39, 39, 39
    };

    private void compressValue(long value) {

        final int Flag1 = 24;

        final int Flag2 = 22;
            int lastsignificantBits = v;
            long xor = storedVal ^ value;
            if (xor == 0) {
                // Write 0
                out.writeBit(false);
                out.writeBit(false);
                size += 2;
                storedLeadingZeros = 65;
            } else {
                int leadingZeros = leadingRound[Long.numberOfLeadingZeros(xor)];
                int trailingZeros = Long.numberOfTrailingZeros(xor);

                if (trailingZeros > THRESHOLD) {
                    //int lastsignificantBits = significantBits;////////////////////////////////////////////////////////
                    int significantBits = 64 - leadingZeros - trailingZeros;
                    out.writeBit(false);
                    out.writeBit(true);
                    //System.out.println(leadingRepresentation[Flag1]);//
                    //storedLeadingZeros = 65;
                    out.writeInt(leadingRepresentation[leadingZeros], 3);//
                    //out.writeInt(significantBits, 6);
                    if (leadingZeros >= 12) {
                        if (significantBits == lastsignificantBits) {
                            out.writeInt(leadingRepresentation[Flag2], 3);
                            out.writeLong(xor >>> trailingZeros, significantBits);
                            size += 8 + significantBits;
                        } else {
                            if (xor == lastXorValue) {///////////////////////////////
                                out.writeInt(leadingRepresentation[Flag1], 3);
                                size += 3;

                            } else if(significantBits - v == 2){
                                    out.writeInt(leadingRepresentation[Flag2], 3);
                                    out.writeLong(xor >>> trailingZeros, significantBits);
                                    size += 10 + significantBits;
                                }else if(significantBits - v == 1) {
                                    out.writeInt(leadingRepresentation[Flag2], 3);
                                    out.writeLong(xor >>> trailingZeros, significantBits);
                                    size += 9 + significantBits;
                                }else {/////////////////////////////////
                                    out.writeInt(significantBits, 6);
                                    out.writeLong(xor >>> trailingZeros, significantBits); // Store the meaningful bits of XOR
                                    size += 11 + significantBits;
                            }
                        }///////////////////////////////
                    }else {
                        out.writeInt(significantBits, 6);
                        out.writeLong(xor >>> trailingZeros, significantBits); // Store the meaningful bits of XOR
                        size += 11 + significantBits;

                    }
                    storedLeadingZeros = 65;///////////////////////
                    v = significantBits;

                } else if (leadingZeros == storedLeadingZeros) {//
                    out.writeBit(true);
                    out.writeBit(false);
                    int significantBits = 64 - leadingZeros;
                    out.writeLong(xor, significantBits);
                    size += 2 + significantBits;
                    v = significantBits;
                } else {
                    storedLeadingZeros = leadingZeros;
                    int significantBits = 64 - leadingZeros;
                    out.writeBit(true);
                    out.writeBit(true);
                    out.writeInt(leadingRepresentation[leadingZeros], 3);
                    out.writeLong(xor, significantBits);
                    size += 5 + significantBits;
                    v = significantBits;
                }
                lastXorValue = xor;
            }
            storedVal = value;
        }

    public int getSize() {
    	return size;
    }
	public byte[] getOut() {
		return out.buffer;
	}
}
