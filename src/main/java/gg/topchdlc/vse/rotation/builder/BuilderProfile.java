package gg.topchdlc.vse.rotation.builder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gg.topchdlc.Client;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Create by daun kvass
 */
public class BuilderProfile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public String name = "Default";

    public float avgSpeed = 0.42f;
    public float maxSpeed = 22.0f;
    public float yawPitchRatio = 1.65f;
    public float dynamicRatio = 1.60f;
    public float accelRatio = 0.35f;
    public float inertia = 0.45f;
    public float accelLimit = 2.4f;
    public float maxJerkLimit = 0.30f;
    public float kineticFriction = 0.75f;
    public float submovementDamping = 0.18f;
    public float arcCurvature = 0.025f;
    public float[] pathArcCurve = new float[10];
    public float wobbleFrequency = 2.8f;
    public float wobbleAmplitude = 0.012f;
    public float jitterIntensity = 0.008f;
    public float microJitter = 0.008f;
    public float macroSway = 0.010f;
    public float overshootFactor = 0.02f;
    public float trackingTremor = 0.010f;
    public float screenShakeIntensity = 0.010f;
    public float strafeReactionLag = 0.08f;
    public float tightSpaceDriftSpeed = 0.025f;
    public float tightSpaceRadius = 0.020f;
    public float[] relativePointX = new float[15];
    public float[] relativePointY = new float[15];

    public float predictionWeight = 0.18f;
    public float pointSmoothness = 0.090f;

    public BuilderProfile() {
        for (int i = 0; i < 15; i++) {
            relativePointX[i] = ((i % 5) - 2) * 0.035f;
            relativePointY[i] = 0.54f + (i / 5) * 0.025f;
        }
        for (int i = 0; i < 10; i++) {
            pathArcCurve[i] = (float) Math.sin((i / 9.0) * Math.PI) * 0.025f;
        }
    }

    public static File getProfileFile(String name) {
        File dir = Client.CLIENT_DIR.resolve("builder_profiles").toFile();
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, name + ".json");
    }

    public void save() {
        try (FileWriter writer = new FileWriter(getProfileFile(name))) {
            GSON.toJson(this, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static BuilderProfile load(String name) {
        File file = getProfileFile(name);
        if (!file.exists()) return defaultProfile();
        try (FileReader reader = new FileReader(file)) {
            return GSON.fromJson(reader, BuilderProfile.class);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultProfile();
        }
    }

    public static BuilderProfile defaultProfile() {
        return new BuilderProfile();
    }
}