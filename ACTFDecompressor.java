package gr.aueb.delorean.chimp;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Decompresses a compressed stream created by the Compressor. Returns pairs of timestamp and floating point value.
 *
 */
public class ACTFDecompressor {
    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private long storedVal = 0;
    private boolean first = true;
    private boolean endOfStream = false;

    private InputBitStream in;

    private final static long NAN_LONG = 0x7ff8000000000000L;
    public final static short[] leadingRepresentation = {0 ,21, 24, 27, 30, 33, 35, 39};
    public final static short[] CenterRepresentation = {0, 8, 16, 24, 32, 40};

    public ACTFDecompressor(byte[] bs) {
        in = new InputBitStream(bs);
    }
	
    private static int LastSignificantBits = 0;
    static int lastflag;
    static int significantBits = 0;
    private void nextValue() throws IOException {//third
    	long value;
        // Read value
    	int flag = in.readInt(2);
    	switch(flag) {
            case 3:
                // New leading zeros
                //LastSignificantBits = significantBits;///////////////
                storedLeadingZeros = leadingRepresentation[in.readInt(3)];
                significantBits = 64 - storedLeadingZeros;
                if(significantBits == 0) {
                    significantBits = 64;
                }
                value = in.readLong(64 - storedLeadingZeros);
                value = storedVal ^ value;
                if (value == NAN_LONG) {
                    endOfStream = true;
                    return;
                } else {
                    storedVal = value;
                }
                break;
            case 2:
                //LastSignificantBits = significantBits;////////////
                significantBits = 64 - storedLeadingZeros;
                if(significantBits == 0) {
                    significantBits = 64;
                }
                value = in.readLong(64 - storedLeadingZeros);
                value = storedVal ^ value;
                if (value == NAN_LONG) {
                    endOfStream = true;
                    return;
                } else {
                    storedVal = value;
                }
                break;
    	    case 1:
            //int Newflag = Newdata & 0x2f;
            //int Newflag = Newdata >> 2;
//            LastSignificantBits = significantBits;////
            storedLeadingZeros = leadingRepresentation[in.readInt(3)];
            //significantBits = in.readInt(6);
            int Newflag = in.readInt(3);///////////////
            //int Newsig = in.readInt(9);
            //int Newflag = Newdata & 0x38;
            //int Newflag = Newdata >> 3;
            //int simplesig;
            //if (lastflag == 1 && (Newflag == 6 || Newflag == 7) ){
            if (Newflag == 6 || Newflag == 7) {
                switch (Newflag) {
                    case 6:
                            storedTrailingZeros = 64 - LastSignificantBits - storedLeadingZeros;
                            value = in.readLong(LastSignificantBits);
                            value <<= storedTrailingZeros;
                            value = storedVal ^ value;
                            if (value == NAN_LONG) {
                                endOfStream = true;
                                return;
                            } else {
                                storedVal = value;
                            }
                            break;

                    case 7:////////////////////////
                        if (LastSignificantBits == 0) {
                            LastSignificantBits = 64;
                        }
                        storedTrailingZeros = 64 - LastSignificantBits - storedLeadingZeros;
                        value = in.readLong(LastSignificantBits);
                        //value = in.readLong(64 - storedLeadingZeros - 3);
                        value <<= storedTrailingZeros;
                        value = storedVal ^ value;
                        if (value == NAN_LONG) {
                            endOfStream = true;
                            return;
                        } else {
                            storedVal = value;
                        }
                        break;
                }
            } else {
                    int Newdata = in.readInt(3);
                    int PsignificantBits = CenterRepresentation[Newflag];
                significantBits = PsignificantBits + Newdata;
                }
                if (significantBits == 0) {
                    significantBits = 64;
                }
                storedTrailingZeros = 64 - significantBits - storedLeadingZeros;
                value = in.readLong(significantBits);
                //value = in.readLong(LastSignificantBits);
                value <<= storedTrailingZeros;
                value = storedVal ^ value;
                if (value == NAN_LONG) {
                    endOfStream = true;
                    return;
                } else {
                    storedVal = value;
                }
            }
    		break;
		default:
    	}
        lastflag = flag;
        LastSignificantBits = significantBits;
    }

}
