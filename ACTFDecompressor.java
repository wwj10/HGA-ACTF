package gr.aueb.delorean.chimp;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Decompresses a compressed stream created by the Compressor. Returns pairs of timestamp and floating point value.
 *
 */
public class ACTFDecompressor {
//    public final static short[] CenterRepresentation = {0, 0, 0, 0, 0, 0, 0, 0,
//            1, 1, 1, 1, 2, 2, 2, 2,
//            3, 3, 4, 4, 5, 5, 6, 6,
//            7, 7, 7, 7, 7, 7, 7, 7,
//            7, 7, 7, 7, 7, 7, 7, 7,
//            7, 7, 7, 7, 7, 7, 7, 7,
//            7, 7, 7, 7, 7, 7, 7, 7,
//            7, 7, 7, 7, 7, 7, 7, 7
//    };
//
//    //对应的舍入值，用于在压缩过程中对数据进行舍入以减少压缩后的数据量。
//    public final static short[] CenterRound = {0, 0, 0, 0, 0, 0, 0, 0,
//            8, 8, 8, 8, 16, 16, 16, 16,
//            24, 24, 32, 32, 40, 40, 48, 48,
//            56, 56, 56, 56, 56, 56, 56, 56,
//            56, 56, 56, 56, 56, 56, 56, 56,
//            56, 56, 56, 56, 56, 56, 56, 56,
//            56, 56, 56, 56, 56, 56, 56, 56,
//            56, 56, 56, 56, 56, 56, 56, 56
//    };
    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private long storedVal = 0;
    private boolean first = true;
    private boolean endOfStream = false;

    private InputBitStream in;

    private final static long NAN_LONG = 0x7ff8000000000000L;

//	public final static short[] leadingRepresentation = {0, 8, 12, 16, 18, 20, 22, 24};
//public final static short[] leadingRepresentation = {0 ,4, 7, 8, 10, 11, 13, 16};
//public final static short[] leadingRepresentation = {0 ,21, 24, 27, 30, 33, 35, 39};//iot1-4
public final static short[] leadingRepresentation = {0 ,21, 24, 27, 30, 33, 35, 39};
public final static short[] CenterRepresentation = {0, 8, 16, 24, 32, 40};


    public ACTFDecompressor(byte[] bs) {
        in = new InputBitStream(bs);//in用于读取压缩后的数据流
    }

    public List<Double> getValues() {//fourth
    	List<Double> list = new LinkedList<>();
    	Double value = readValue();
    	while (value != null) {
    		list.add(value);
    		value = readValue();
    	}
    	return list;
    }
    
    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    public Double readValue() {//first
        try {
			next();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
        if(endOfStream) {
            return null;
        }
        return Double.longBitsToDouble(storedVal);
    }
    //second,在 next() 方法中，首先检查是否是第一次调用该方法。如果是第一次调用，则直接从输入流中读取 64 位，并将其存储为 storedVal。
    // 如果 storedVal 的值等于 NAN_LONG（表示结束标志），则将 endOfStream 设置为 true，并返回。
    // 如果不是第一次调用 next() 方法，那么会根据不同的标志位进行相应的操作
    private void next() throws IOException {
        if (first) {
        	first = false;
            storedVal = in.readLong(64);
             if (storedVal == NAN_LONG) {
            	endOfStream = true;
            	//return;
            }

        } else {
        	nextValue();
        }
    }
    private static int LastSignificantBits = 0;
    static int lastflag;
    static int significantBits = 0;
    private void nextValue() throws IOException {//third
    	//int significantBits = 0;
    	long value;
        // Read value
        //从输入流in中读取一个2位的整数，并将其赋值给变量flag
    	int flag = in.readInt(2);
        //1、若flag为3，则表示有新的前导零。首先根据读取的3位整数选择相应的前导零数量，并计算出有效位数significantBits。
        // 如果significantBits等于0，则将其设置为64。接着从输入流中读取64-storedLeadingZeros位长的长整型值，并将其与storedVal进行异或操作，得到新的value值。
        // 如果value等于NAN_LONG，表示到达了流的末尾，将endOfStream标志设置为true并返回；否则将value赋值给storedVal。
        //2、若flag为2，则表示没有新的前导零。根据已存储的前导零数计算出有效位数significantBits。如果significantBits等于0，则将其设置为64。
        // 从输入流中读取64-storedLeadingZeros位长的长整型值，并将其与storedVal进行异或操作，得到新的value值。
        // 如果value等于NAN_LONG，表示到达了流的末尾，将endOfStream标志设置为true并返回；否则将value赋值给storedVal。
        //3、若flag为1，则表示有新的前导零和有效位数。首先根据读取的3位整数选择相应的前导零数量，并计算出有效位数significantBits。
        // 如果significantBits等于0，则将其设置为64。计算存储的尾随零的数量storedTrailingZeros，然后从输入流中读取64-storedLeadingZeros-storedTrailingZeros位长的长整型值，
        // 并将其左移storedTrailingZeros位，再与storedVal进行异或操作，得到新的value值。如果value等于NAN_LONG，表示到达了流的末尾，将endOfStream标志设置为true并返回；否则将value赋值给storedVal。
        //4、若flag不是3、2、1中的任何一个值，则没有任何操作。
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
                        //simplesig = Newsig >> 6;
                        //simplesig = Newsig & 0x6f;
                        //value = in.readLong(64 - storedLeadingZeros - 3 - simplesig);//
//                        if(significantBits - LastSignificantBits == 2 && significantBits - LastSignificantBits == 1){
//                            storedTrailingZeros = 64 - significantBits - storedLeadingZeros;
//                            value = in.readLong(significantBits);
//                            value <<= storedTrailingZeros;
//                            value = storedVal ^ value;
//                            if (value == NAN_LONG) {
//                                endOfStream = true;
//                                return;
//                            } else {
//                                storedVal = value;
//                            }
//                            break;
//                        }else {/////////////////////////////////
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
//                        }

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
            } else {{
//                if (Newflag == 6 || Newflag == 7) {
//                    significantBits = in.readInt(LastSignificantBits);////////////////////可能需要将LastSignificantBits左移三位。
//                //significantBits = Newdata & 0x3f;
//                } else {
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
