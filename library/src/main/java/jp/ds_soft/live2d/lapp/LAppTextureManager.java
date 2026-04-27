package jp.ds_soft.live2d.lapp;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import com.live2d.sdk.cubism.framework.CubismFramework;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class LAppTextureManager {
    public static class TextureInfo {
        public int id;
        public int width;
        public int height;
        public String filePath;
    }

    public TextureInfo createTextureFromPngFile(String filePath) {
        for (TextureInfo textureInfo : textures) {
            if (textureInfo.filePath.equals(filePath)) {
                return textureInfo;
            }
        }

        AssetManager assetManager = LAppDelegate.getInstance().getContext().getAssets();
        InputStream stream = null;
        try {
            stream = assetManager.open(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap bitmap = BitmapFactory.decodeStream(stream);
        if (bitmap == null) {
            return null;
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        int[] textureId = new int[1];
        GLES20.glGenTextures(1, textureId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        TextureInfo textureInfo = new TextureInfo();
        textureInfo.filePath = filePath;
        textureInfo.width = bitmap.getWidth();
        textureInfo.height = bitmap.getHeight();
        textureInfo.id = textureId[0];

        textures.add(textureInfo);
        bitmap.recycle();

        if (LAppDefine.DEBUG_LOG_ENABLE) {
            CubismFramework.coreLogFunction("Create texture: " + filePath);
        }

        return textureInfo;
    }

    public void releaseTextures() {
        for (TextureInfo textureInfo : textures) {
            GLES20.glDeleteTextures(1, new int[]{textureInfo.id}, 0);
        }
        textures.clear();
    }

    private final List<TextureInfo> textures = new ArrayList<TextureInfo>();
}
