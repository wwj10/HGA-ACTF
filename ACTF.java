package gr.aueb.delorean.chimp;

/**
 * Implements the Chimp time series compression. Value compression
 * is for floating points only.
 *
 * @author Panagiotis Liakos
 */
public class ACTF {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private long storedVal = 0;
    private boolean first = true;
    private int size;
//    private final int Flag1 = 0b00000111;
    private long lastXorValue = 0;
    //    private int Flag2 = 0b00000110;
    private int significantBits = 0;
    private int v = 0;
    public final static int THRESHOLD = 6;
    
//    public final static short[] leadingRepresentation = {0, 0, 0, 0, 0, 0, 0, 0,
//			1, 1, 1, 1, 2, 2, 2, 2,
//			3, 3, 4, 4, 5, 5, 6, 6,
//			7, 7, 7, 7, 7, 7, 7, 7,
//			7, 7, 7, 7, 7, 7, 7, 7,
//			7, 7, 7, 7, 7, 7, 7, 7,
//			7, 7, 7, 7, 7, 7, 7, 7,
//			7, 7, 7, 7, 7, 7, 7, 7
//		};

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

//    public final static short[] leadingRound = {0, 0, 0, 0, 0, 0, 0, 0,
//			8, 8, 8, 8, 12, 12, 12, 12,
//			16, 16, 18, 18, 20, 20, 22, 22,
//			24, 24, 24, 24, 24, 24, 24, 24,
//			24, 24, 24, 24, 24, 24, 24, 24,
//			24, 24, 24, 24, 24, 24, 24, 24,
//			24, 24, 24, 24, 24, 24, 24, 24,
//			24, 24, 24, 24, 24, 24, 24, 24
//		};

//    public final static short FIRST_DELTA_BITS = 27;

    private OutputBitStream out;//已阅读

    // We should have access to the series?
    public ACTF() {
        out = new OutputBitStream(new byte[1000*8]);
        size = 0;
    }

    /**
     * Adds a new long value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     */
    public void addValue(long value) {
        if(first) {
            writeFirst(value);
        } else {
            compressValue(value);
        }
    }

    /**
     * Adds a new double value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     */
    public void addValue(double value) {
        if(first) {
            writeFirst(Double.doubleToRawLongBits(value));//
        } else {
            compressValue(Double.doubleToRawLongBits(value));//
        }
    }

    private void writeFirst(long value) {
    	first = false;
        storedVal = value;
        out.writeLong(storedVal, 64);
        size += 64;
    }

    /**
     * Closes the block and writes the remaining stuff to the BitOutput.
     */
    public void close() {
    	addValue(Double.NaN);
    	out.writeBit(false);
        out.flush();
    }
    private void compressValue(long value) {

        //test
        final int Flag1 = 24;

        final int Flag2 = 22;
//        if (value == storedVal) {
//            out.writeBit(false);
//            out.writeBit(false);
//            size += 2;
//            storedLeadingZeros = 65;////////////////////////
//        }
//         else {
            int lastsignificantBits = v;
            long xor = storedVal ^ value;
            if (xor == 0) {
                // Write 0
                out.writeBit(false);
//                out.writeBit(false);
                size += 1;
                storedLeadingZeros = 65;
            } else {
                //else{
                int leadingZeros = leadingRound[Long.numberOfLeadingZeros(xor)];
                //System.out.println(Long.numberOfLeadingZeros(xor));
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
//                    if (leadingZeros >= 8 && leadingZeros < 12) {
//
//                    }
                    ////////////////////////
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
    //}

    public int getSize() {
    	return size;
    }

	public byte[] getOut() {
		return out.buffer;
	}
}
