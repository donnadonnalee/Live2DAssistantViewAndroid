package jp.ds_soft.live2d.lapp;

import com.live2d.sdk.cubism.framework.math.CubismMatrix44;
import com.live2d.sdk.cubism.framework.motion.ACubismMotion;
import com.live2d.sdk.cubism.framework.motion.IBeganMotionCallback;
import com.live2d.sdk.cubism.framework.motion.IFinishedMotionCallback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.content.res.AssetManager;
import static jp.ds_soft.live2d.lapp.LAppDefine.*;

public class LAppLive2DManager {
    public static LAppLive2DManager getInstance() {
        if (s_instance == null) {
            s_instance = new LAppLive2DManager();
        }
        return s_instance;
    }

    public static void releaseInstance() {
        s_instance = null;
    }

    public void releaseAllModel() {
        for (LAppModel model : models) {
            model.deleteModel();
        }
        models.clear();
    }

    public void changeModel(String modelName) {
        releaseAllModel();
        String modelPath = modelName + "/";
        String modelJsonName = modelName + ".model3.json";
        LAppModel model = new LAppModel();
        model.loadAssets(modelPath, modelJsonName);
        models.add(model);
    }

    public void setUpModel() {
        modelDir.clear();
        final AssetManager assets = LAppDelegate.getInstance().getContext().getAssets();
        try {
            String[] root = assets.list("");
            for (String subdir: root) {
                String[] files = assets.list(subdir);
                String target = subdir + ".model3.json";
                for (String file : files) {
                    if (file.equals(target)) {
                        modelDir.add(subdir);
                        break;
                    }
                }
            }
            Collections.sort(modelDir);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public void setViewMatrix(CubismMatrix44 matrix) {
        viewMatrix.setMatrix(matrix.getArray());
    }

    public void onUpdate() {
        int width = LAppDelegate.getInstance().getWindowWidth();
        int height = LAppDelegate.getInstance().getWindowHeight();

        if (models.size() == 0) {
            LAppPal.printLog("No models loaded in LAppLive2DManager.");
        }

        for (int i = 0; i < models.size(); i++) {
            LAppModel model = models.get(i);
            if (model.getModel() == null) {
                LAppPal.printLog("Model " + i + " has no internal model data (null).");
                continue;
            }
            
            projection.loadIdentity();
            if (model.getModel().getCanvasWidth() > 1.0f && width < height) {
                model.getModelMatrix().setWidth(2.0f);
                projection.scale(1.0f, (float) width / (float) height);
            } else {
                projection.scale((float) height / (float) width, 1.0f);
            }

            if (viewMatrix != null) {
                projection.multiplyByMatrix(viewMatrix);
            }
            
            // 描画実行のログ
            // LAppPal.printLog("Drawing model " + i + " with projection matrix: " + projection.toString());
            
            LAppDelegate.getInstance().getView().preModelDraw(model);
            model.update();
            model.draw(projection);
            LAppDelegate.getInstance().getView().postModelDraw(model);
        }
    }

    public void onDrag(float x, float y) {
        for (int i = 0; i < models.size(); i++) {
            LAppModel model = getModel(i);
            if (model != null) model.setDragging(x, y);
        }
    }

    public void onTap(float x, float y) {
        LAppPal.printLog("Tap event at: " + x + ", " + y);
        for (int i = 0; i < models.size(); i++) {
            LAppModel model = models.get(i);
            if (model != null && model.getModel() != null) {
                if (model.hitTest(LAppDefine.HitAreaName.HEAD.getId(), x, y)) {
                    LAppPal.printLog("Hit test: HEAD");
                    model.setRandomExpression();
                } else if (model.hitTest(LAppDefine.HitAreaName.BODY.getId(), x, y)) {
                    LAppPal.printLog("Hit test: BODY");
                    model.startRandomMotion(LAppDefine.MotionGroup.TAP_BODY.getId(), LAppDefine.Priority.NORMAL.getPriority(), finishedMotion, beganMotion);
                } else {
                    LAppPal.printLog("Hit test: NONE");
                }
            }
        }
    }

    public void nextScene() {
        final int number = (currentModel + 1) % modelDir.size();
        changeScene(number);
    }

    public void changeScene(int index) {
        if (index >= modelDir.size()) return;
        changeModelByName(modelDir.get(index));
    }

    public void changeModelByName(String modelName) {
        AssetManager assets = LAppDelegate.getInstance().getContext().getAssets();
        String modelPath = modelName + "/";
        String modelJsonName = findModelJson(assets, modelName);

        // runtimeフォルダ内も探す
        if (modelJsonName == null) {
            modelPath = modelName + "/runtime/";
            modelJsonName = findModelJson(assets, modelName + "/runtime");
        }

        if (modelJsonName == null) {
            LAppPal.printLog("Model JSON not found in " + modelName);
            return;
        }

        releaseAllModel();
        LAppModel model = new LAppModel();
        model.loadAssets(modelPath, modelJsonName);
        models.add(model);
    }

    private String findModelJson(AssetManager assets, String path) {
        try {
            String[] files = assets.list(path);
            if (files != null) {
                for (String file : files) {
                    if (file.endsWith(".model3.json")) {
                        return file;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LAppModel getModel(int number) {
        if (number < models.size()) {
            return models.get(number);
        }
        return null;
    }

    public int getModelNum() {
        return models.size();
    }

    private static class BeganMotion implements IBeganMotionCallback {
        @Override
        public void execute(ACubismMotion motion) {}
    }
    private static final BeganMotion beganMotion = new BeganMotion();

    private static class FinishedMotion implements IFinishedMotionCallback {
        @Override
        public void execute(ACubismMotion motion) {}
    }
    private static final FinishedMotion finishedMotion = new FinishedMotion();

    private static LAppLive2DManager s_instance;

    private LAppLive2DManager() {
        setUpModel();
    }

    private final List<LAppModel> models = new ArrayList<>();
    private int currentModel;
    private final List<String> modelDir = new ArrayList<>();
    private final CubismMatrix44 viewMatrix = CubismMatrix44.create();
    private final CubismMatrix44 projection = CubismMatrix44.create();
}
