/**
 * ͼƬ���ؼ�ת������ ----------------------------------------------------------------------- ���죺һ��Bitmap����ռ�ö���ڴ棿ϵͳ��ÿ��Ӧ�ó���������ڴ棿 Bitmapռ�õ��ڴ�Ϊ����������
 * * ÿ������ռ�õ��ڴ档��Android�У� Bitmap�������������ͣ�ARGB_8888��ARGB_4444��ARGB_565��ALPHA_8�� ����ÿ������ռ�õ��ֽ����ֱ�Ϊ4��2��2��1����ˣ�һ��2000*1000��ARGB_8888
 * ���͵�Bitmapռ�õ��ڴ�Ϊ2000*1000*4=8000000B=8MB��
 * 
 * @author chen.lin
 *
 */
public class BitmapUtil {

    /**
     *   1)������ ,�Ѿ����ʺϻ���ͼƬ��Ϣ,����ͼƬʱ������ص�������
     *   2)Android 3.0 (API Level 11)�У�ͼƬ�����ݻ�洢�ڱ��ص��ڴ浱��
     *   ����޷���һ�ֿ�Ԥ���ķ�ʽ�����ͷţ������Ǳ�ڵķ������Ӧ�ó�����ڴ������������
     *   3)��Ϊ�� Android 2.3 (API Level 9)��ʼ��������������������ڻ��ճ��������û������õĶ���
            ���������ú������ñ�ò��ٿɿ���
     *   
     */
    private static Map<String, SoftReference<Bitmap>> imageCache = new HashMap<String, SoftReference<Bitmap>>();

    /**
     *  ��ʼ��lrucache,����ʹ�������Ƴ�,LruCache������ͼƬ��
     *  ���洢Image�Ĵ�С����LruCache�趨��ֵ��ϵͳ�Զ��ͷ��ڴ棬
     */
    private static LruCache<String, Bitmap> mMemoryCache;
    static {
        final int memory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = memory / 8;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            protected int sizeOf(String key, Bitmap value) {
                // return value.getByteCount() / 1024;
                return value.getHeight() * value.getRowBytes();
            }
        };
    }

    // ---lrucache----------------------------------------------------
    /**
     * ���ͼƬ��lrucache
     * 
     * @param key
     * @param bitmap
     */
    public synchronized void addBitmapToMemCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            if (key != null & bitmap != null) {
                mMemoryCache.put(key, bitmap);
            }
        }
    }

    /**
     * �������
     */
    public void clearMemCache() {
        if (mMemoryCache != null) {
            if (mMemoryCache.size() > 0) {
                mMemoryCache.evictAll();
            }
            mMemoryCache = null;
        }
    }

    /**
     * �Ƴ�����
     */
    public synchronized void removeMemCache(String key) {
        if (key != null) {
            if (mMemoryCache != null) {
                Bitmap bm = mMemoryCache.remove(key);
                if (bm != null)
                    bm.recycle();
            }
        }
    }

    /**
     * ��lrucache���ȡͼƬ
     * 
     * @param key
     * @return
     */
    public Bitmap getBitmapFromMemCache(String key) {
        if (key != null) {
            return mMemoryCache.get(key);
        }
        return null;
    }

    /**
     * ����ͼƬ
     * 
     * @param context
     * @param resId
     * @param imageView
     */
    public void loadBitmap(Context context, int resId, ImageView imageView) {
        final String imageKey = String.valueOf(resId);
        final Bitmap bitmap = getBitmapFromMemCache(imageKey);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(resId);
            BitmapWorkerTask task = new BitmapWorkerTask(context);
            task.execute(resId);
        }
    }

    /**
     * ������
     * 
     * @Project App_View
     * @Package com.android.view.tool
     * @author chenlin
     * @version 1.0
     * @Date 2014��5��10��
     */
    class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {
        private Context mContext;

        public BitmapWorkerTask(Context context) {
            mContext = context;
        }

        // �ں�̨����ͼƬ��
        @Override
        protected Bitmap doInBackground(Integer... params) {
            final Bitmap bitmap = decodeSampledBitmapFromResource(mContext.getResources(), params[0], 100, 100);
            addBitmapToMemCache(String.valueOf(params[0]), bitmap);
            return bitmap;
        }
    }

    // --������---------------------------------------------------------
    public static void addBitmapToCache(String path) {
        // ǿ���õ�Bitmap����
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        // �����õ�Bitmap����
        SoftReference<Bitmap> softBitmap = new SoftReference<Bitmap>(bitmap);
        // ��Ӹö���Map��ʹ�仺��
        imageCache.put(path, softBitmap);
    }

    public static Bitmap getBitmapByPath(String path) {
        // �ӻ�����ȡ�����õ�Bitmap����
        SoftReference<Bitmap> softBitmap = imageCache.get(path);
        // �ж��Ƿ����������
        if (softBitmap == null) {
            return null;
        }
        // ȡ��Bitmap������������ڴ治��Bitmap�����գ���ȡ�ÿ�
        Bitmap bitmap = softBitmap.get();
        return bitmap;
    }

    public Bitmap loadBitmap(final String imageUrl, final ImageCallBack imageCallBack) {
        SoftReference<Bitmap> reference = imageCache.get(imageUrl);
        if (reference != null) {
            if (reference.get() != null) {
                return reference.get();
            }
        }
        final Handler handler = new Handler() {
            public void handleMessage(final android.os.Message msg) {
                // ���뵽������
                Bitmap bitmap = (Bitmap) msg.obj;
                imageCache.put(imageUrl, new SoftReference<Bitmap>(bitmap));
                if (imageCallBack != null) {
                    imageCallBack.getBitmap(bitmap);
                }
            }
        };
        new Thread() {
            public void run() {
                Message message = handler.obtainMessage();
                message.obj = downloadBitmap(imageUrl);
                handler.sendMessage(message);
            }
        }.start();
        return null;
    }

    public interface ImageCallBack {
        void getBitmap(Bitmap bitmap);
    }

    // ----��������----------------------------------------------------------------------------------
    /**
     * ����������ͼƬ
     * 
     * @param imageUrl
     * @return
     */
    private Bitmap downloadBitmap(String imageUrl) {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(new URL(imageUrl).openStream());
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * drawable תbitmap
     * 
     * @param drawable
     * @return
     */
    public static Bitmap drawable2Bitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        // canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * bitmap ת drawable
     * 
     * @param bm
     * @return
     */
    public static Drawable bitmap2Drable(Bitmap bm) {
        return new BitmapDrawable(bm);
    }

    /**
     * ���ֽ�����ͨ��BASE64Encoderת�����ַ���
     * 
     * @param image
     * @return
     */
    public static String getBase64(byte[] image) {
        String string = "";
        try {
            BASE64Encoder encoder = new BASE64Encoder();
            string = encoder.encodeBuffer(image).trim();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return string;
    }

    /**
     * ���ֽ�����ת����Drawable
     * 
     * @param imgByte
     *            �ֽ�����
     * @return
     */
    @SuppressWarnings("deprecation")
    public static Drawable byte2Drawable(byte[] imgByte) {
        Bitmap bitmap;
        if (imgByte != null) {
            bitmap = BitmapFactory.decodeByteArray(imgByte, 0, imgByte.length);
            Drawable drawable = new BitmapDrawable(bitmap);

            return drawable;
        }
        return null;
    }

    /**
     * ��ͼƬת�����ֽ�����
     * 
     * @param bmp
     * @return
     */
    public static byte[] bitmap2Byte(Bitmap bm) {
        Bitmap outBitmap = Bitmap.createScaledBitmap(bm, 150, bm.getHeight() * 150 / bm.getWidth(), true);
        if (bm != outBitmap) {
            bm.recycle();
            bm = null;
        }
        byte[] compressData = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            try {
                outBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            } catch (Exception e) {
                e.printStackTrace();
            }
            compressData = baos.toByteArray();
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return compressData;
    }

    /**
     * ����ͼƬ
     * 
     * @param bitmap
     *            ԭͼƬ
     * @param newWidth
     * @param newHeight
     * @return
     */
    public static Bitmap setBitmapSize(Bitmap bitmap, int newWidth, int newHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = (newWidth * 1.0f) / width;
        float scaleHeight = (newHeight * 1.0f) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    /**
     * ����ͼƬ
     * 
     * @param bitmapPath
     *            ͼƬ·��
     * @return
     */
    public static Bitmap setBitmapSize(String bitmapPath, float newWidth, float newHeight) {
        Bitmap bitmap = BitmapFactory.decodeFile(bitmapPath);
        if (bitmap == null) {
            Logger.i("bitmap", "bitmap------------>����δ֪�쳣��");
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = newWidth / width;
        float scaleHeight = newHeight / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    /**
     * ����ͼƬ�����Ŵ�С ���==1����ʾû�仯��==2����ʾ��߶���Сһ�� ----------------------------------------------------------------------------
     * inSampleSize��BitmapFactory.Options���һ���������ò���Ϊint�ͣ� ����ֵָʾ���ڽ���ͼƬΪBitmapʱ�ڳ�������������������С�ı�����inSampleSize��Ĭ��ֵ����СֵΪ1����С��1ʱ������������ֵ����1��������
     * ���ڴ���1ʱ����ֵֻ��Ϊ2���ݣ�����Ϊ2����ʱ����������ȡ���ֵ��ӽ���2���ݣ��� ���磬��inSampleSizeΪ2ʱ��һ��2000*1000��ͼƬ��������СΪ1000*500����Ӧ�أ� �������������ڴ�ռ�ö�����СΪ��ԭ����1/4��
     * 
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // ԭʼͼƬ�Ŀ��
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // �ڱ�֤��������bitmap��߷ֱ����Ŀ��ߴ��ߵ�ǰ���£�ȡ���ܵ�inSampleSize�����ֵ
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * ���ݼ������inSampleSize����Bitmap(��ʱ��bitmap�Ǿ������ŵ�ͼƬ)
     * 
     * @param res
     * @param resId
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        // �������� inJustDecodeBounds=true ����ȡͼƬ�ߴ�
        final BitmapFactory.Options options = new BitmapFactory.Options();
        /**
         * inJustDecodeBounds��������Ϊtrue��decodeResource()�����Ͳ�������Bitmap���󣬶������Ƕ�ȡ��ͼƬ�ĳߴ��������Ϣ��
         */
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // ���� inSampleSize ��ֵ
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // ���ݼ������ inSampleSize ������ͼƬ����Bitmap
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * ��ͼƬ���浽����ʱ����ѹ��, ����ͼƬ��Bitmap��ʽ��ΪFile��ʽʱ����ѹ��, 
     * �ص���: File��ʽ��ͼƬȷʵ��ѹ����, ���ǵ������¶�ȡѹ�����fileΪ Bitmap��,��ռ�õ��ڴ沢û�иı�
     * 
     * @param bmp
     * @param file
     */
    public static void compressBmpToFile(Bitmap bmp, File file) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int options = 80;// ����ϲ����80��ʼ,
        bmp.compress(Bitmap.CompressFormat.JPEG, options, baos);
        while (baos.toByteArray().length / 1024 > 100) {
            baos.reset();
            options -= 10;
            bmp.compress(Bitmap.CompressFormat.JPEG, options, baos);
        }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *  ��ͼƬ�ӱ��ض����ڴ�ʱ,����ѹ�� ,��ͼƬ��File��ʽ��ΪBitmap��ʽ
     *  �ص�: ͨ�����ò�����, ����ͼƬ������, �ﵽ���ڴ��е�Bitmap����ѹ��
     * @param srcPath
     * @return
     */
    public static Bitmap compressImageFromFile(String srcPath, float pixWidth, float pixHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;// ֻ����,��������
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath, options);

        options.inJustDecodeBounds = false;
        int w = options.outWidth;
        int h = options.outHeight;
        int scale = 1;
        if (w > h && w > pixWidth) {
            scale = (int) (options.outWidth / pixWidth);
        } else if (w < h && h > pixHeight) {
            scale = (int) (options.outHeight / pixHeight);
        }
        if (scale <= 0)
            scale = 1;
        options.inSampleSize = scale;// ���ò�����

        options.inPreferredConfig = Config.ARGB_8888;// ��ģʽ��Ĭ�ϵ�,�ɲ���
        options.inPurgeable = true;// ͬʱ���òŻ���Ч
        options.inInputShareable = true;// ����ϵͳ�ڴ治��ʱ��ͼƬ�Զ�������

        bitmap = BitmapFactory.decodeFile(srcPath, options);
        // return compressBmpFromBmp(bitmap);//ԭ���ķ������������������ͼ���ж���ѹ��
        // ��ʵ����Ч��,��Ҿ��ܳ���
        return bitmap;
    }

    /**
     * �ж���Ƭ�ĽǶ�
     * @param path
     * @return
     */
    public static int readPictureDegree(String path) {  
        int degree = 0;  
        try {  
            ExifInterface exifInterface = new ExifInterface(path);  
            int orientation = exifInterface.getAttributeInt(  
                    ExifInterface.TAG_ORIENTATION,  
                    ExifInterface.ORIENTATION_NORMAL);  
            switch (orientation) {  
            case ExifInterface.ORIENTATION_ROTATE_90:  
                degree = 90;  
                break;  
            case ExifInterface.ORIENTATION_ROTATE_180:  
                degree = 180;  
                break;  
            case ExifInterface.ORIENTATION_ROTATE_270:  
                degree = 270;  
                break;  
            }  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
        return degree;  
    }  

    /**
     * Android�����豸��Ļ�ߴ��dpi�Ĳ�ͬ����ϵͳ����ĵ�Ӧ�ó����ڴ��СҲ��ͬ���������±�
     * 
     * ��Ļ�ߴ� DPI Ӧ���ڴ� 
     * small / normal / large ldpi / mdpi 16MB 
     * small / normal / large tvdpi / hdpi 32MB 
     * small / normal / large xhdpi 64MB
     * small / normal / large 400dpi 96MB 
     * small / normal / large xxhdpi 128MB 
     * ------------------------------------------------------- 
     * xlarge mdpi 32MB 
     * xlarge tvdpi / hdpi 64MB 
     * xlarge xhdpi 128MB 
     * xlarge 400dpi 192MB 
     * xlarge xxhdpi 256MB
     */

}