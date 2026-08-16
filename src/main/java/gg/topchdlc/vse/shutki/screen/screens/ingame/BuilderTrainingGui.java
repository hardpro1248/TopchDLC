package gg.topchdlc.vse.shutki.screen.screens.ingame;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventKey;
import gg.topchdlc.api.render.RendererObject;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.rotation.builder.BuilderProfile;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Create by daun kvass
 */
public class BuilderTrainingGui extends RendererObject {

    private enum Mode { NAMING, FLICK, TRACKING }
    private Mode currentMode = Mode.NAMING;

    private boolean opened = false;
    private boolean isSaving = false;
    private String profileName = "";

    private final int FLICKS_PER_CYCLE = 15;
    private final float TRACKING_TIME_PER_CYCLE = 7.0f;
    private int flicksInCurrentCycle = 0;
    private float trackingTimer = 0f;
    private int completedCycles = 0;

    private final List<FlickTrack> flicks = new ArrayList<>();
    private FlickTrack currentFlick = null;
    private TargetCircle activeCircle = null;

    private final List<Float> trackingTremors = new ArrayList<>();
    private MovingPatternTarget movingTarget = null;

    private double lastMouseX = -1;
    private double lastMouseY = -1;
    private long lastMoveTime = System.currentTimeMillis();
    private long lastFrameTime = System.currentTimeMillis();

    public BuilderTrainingGui() {
        Client.EVENTS.register(this);
    }

    public boolean isOpened() { return opened; }

    public void setOpened(boolean opened) {
        this.opened = opened;
        if (opened) {
            this.currentMode = Mode.NAMING;
            this.isSaving = false;
            this.profileName = "";
            this.flicks.clear();
            this.trackingTremors.clear();
            this.flicksInCurrentCycle = 0;
            this.trackingTimer = 0f;
            this.completedCycles = 0;
            this.activeCircle = null;
            this.currentFlick = null;
            this.movingTarget = null;
            this.lastFrameTime = System.currentTimeMillis();
            if (mc != null && mc.mouse != null) mc.mouse.unlockCursor();
        } else {
            if (mc != null && mc.mouse != null) mc.mouse.lockCursor();
        }
    }

    EventBus<EventKey> eventKey = event -> {
        if (!opened) return;
        int key = event.getKey();
        if (event.action == 1 || event.action == 2) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                setOpened(false);
                event.cancel();
                return;
            }

            if (currentMode == Mode.NAMING) {
                if (key == GLFW.GLFW_KEY_BACKSPACE && !profileName.isEmpty()) {
                    profileName = profileName.substring(0, profileName.length() - 1);
                } else if (key == GLFW.GLFW_KEY_ENTER && !profileName.isEmpty()) {
                    currentMode = Mode.FLICK;
                    flicks.clear();
                } else if (profileName.length() < 16) {
                    char c = getCharFromKey(key);
                    if (c != 0) profileName += c;
                }
                event.cancel();
            }
        }
    };

    @Override
    public void render(int mouseX, int mouseY) {
        if (!opened || window == null || isSaving) return;

        long now = System.currentTimeMillis();
        float delta = (now - lastFrameTime) / 1000.0f;
        lastFrameTime = now;

        this.bound(0, 0, window.getScaledWidth(), window.getScaledHeight());

        Client.RENDERER.blur(0, 0, width, height, new Vector4f(0), 20f, 1f);
        Color bgColor = new Color(10, 10, 15, 200);
        Client.RENDERER.rect(0, 0, width, height, new Vector4f(0), 1f, bgColor, bgColor, bgColor, bgColor);

        if (currentMode == Mode.NAMING) {
            renderNamingStage(mouseX, mouseY);
        } else if (currentMode == Mode.FLICK) {
            renderFlickStage(mouseX, mouseY);
        } else if (currentMode == Mode.TRACKING) {
            renderTrackingStage(mouseX, mouseY, delta);
        }

        if (currentMode != Mode.NAMING) {
            renderSaveButton(mouseX, mouseY);
        }
    }

    private void renderNamingStage(int mouseX, int mouseY) {
        float cardW = 320, cardH = 170;
        float cardX = (width - cardW) / 2f;
        float cardY = (height - cardH) / 2f;

        Color cardBg = new Color(20, 20, 28, 240);
        Color border = new Color(255, 255, 255, 15);

        Client.RENDERER.rect(cardX, cardY, cardW, cardH, new Vector4f(12), 1f, cardBg, cardBg, cardBg, cardBg);
        Client.RENDERER.outline(cardX, cardY, cardW, cardH, 1f, new Vector4f(12), new Vector2f(1), border, border, border, border);

        Client.RENDERER.textCentered("Builder", cardX + cardW / 2f, cardY + 20, TextureUse.SFMEDIUM, 8f, Color.WHITE);

        float inputX = cardX + 25, inputY = cardY + 60, inputW = cardW - 50, inputH = 30;
        Color inputBg = new Color(12, 12, 18, 250);
        Client.RENDERER.rect(inputX, inputY, inputW, inputH-5, new Vector4f(6), 1f, inputBg, inputBg, inputBg, inputBg);

        String displayText = profileName.isEmpty() ? "Type profile name..." : profileName;
        Color textColor = profileName.isEmpty() ? Color.GRAY : Color.WHITE;
        Client.RENDERER.text(displayText, inputX + 10, inputY + 9, TextureUse.SFMEDIUM, 6.5f, textColor);

        float btnX = cardX + 25, btnY = cardY + 110, btnW = cardW - 50, btnH = 24;
        boolean hover = MathUtility.mouseIn(btnX, btnY, btnW, btnH, mouseX, mouseY);
        Color btnColor = hover ? new Color(80, 140, 255) : new Color(60, 110, 230);

        Client.RENDERER.rect(btnX+5, btnY, btnW-10, btnH, new Vector4f(6), 1f, btnColor, btnColor, btnColor, btnColor);
        Client.RENDERER.textCentered("Go?", btnX + btnW / 2f, btnY + 7, TextureUse.SFMEDIUM, 7f, Color.WHITE);
    }

    private void renderFlickStage(int mouseX, int mouseY) {
        long now = System.currentTimeMillis();
        long dt = now - lastMoveTime;

        if (lastMouseX != -1 && dt > 0 && currentFlick != null) {
            double dx = mouseX - lastMouseX;
            double dy = mouseY - lastMouseY;
            double dist = Math.hypot(dx, dy);

            if (dist > 0.05) {
                float speed = (float) (dist / dt);
                currentFlick.addSample((float) mouseX, (float) mouseY, (float) Math.abs(dx / dt), (float) Math.abs(dy / dt), speed);
            }
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
        lastMoveTime = now;

        if (activeCircle == null) spawnNewCircle(mouseX, mouseY);

        if (currentFlick != null && currentFlick.samples.size() > 1) {
            for (int i = 1; i < currentFlick.samples.size(); i++) {
                PathSample p1 = currentFlick.samples.get(i - 1);
                PathSample p2 = currentFlick.samples.get(i);
                Color lineCol = new Color(80, 180, 255, 160);
                Client.RENDERER.line(p1.x, p1.y, p2.x, p2.y, 2.0f, 1f, 1f, lineCol, lineCol);
            }
        }

        if (activeCircle != null) {
            activeCircle.update(0.016f);
            float r = activeCircle.size * activeCircle.animProgress;

            Color circleColor = new Color(255, 90, 90, 190);
            Client.RENDERER.rect(activeCircle.x - r, activeCircle.y - r, r * 2, r * 2, new Vector4f(r), 1f, circleColor, circleColor, circleColor, circleColor);

            Color centerColor = new Color(255, 255, 255, 230);
            Client.RENDERER.rect(activeCircle.x - 2, activeCircle.y - 2, 4, 4, new Vector4f(2), 1f, centerColor, centerColor, centerColor, centerColor);
        }

        Client.RENDERER.textCentered("Click + samples : " + (flicks.size()), width / 2f, 25, TextureUse.SFMEDIUM, 8f, Color.WHITE);
        Client.RENDERER.textCentered("67 sosiski", width / 2f, 40, TextureUse.SFMEDIUM, 6f, Color.LIGHT_GRAY);
    }

    private void renderTrackingStage(int mouseX, int mouseY, float delta) {
        if (movingTarget == null) {
            movingTarget = new MovingPatternTarget(width / 2f, height / 2f);
        }

        trackingTimer += delta;
        movingTarget.update(delta, width, height);

        double distToTarget = Math.hypot(mouseX - movingTarget.x, mouseY - movingTarget.y);
        trackingTremors.add((float) distToTarget);

        float r = 24f;
        Color col = distToTarget < r ? new Color(100, 255, 120, 200) : new Color(255, 180, 60, 200);
        Client.RENDERER.rect(movingTarget.x - r, movingTarget.y - r, r * 2, r * 2, new Vector4f(r), 1f, col, col, col, col);

        float timeLeft = Math.max(0f, TRACKING_TIME_PER_CYCLE - trackingTimer);
        Client.RENDERER.textCentered(String.format("Hold mouse cube ", timeLeft, "a", flicks.size()), width / 2f, 25, TextureUse.SFMEDIUM, 8f, Color.WHITE);
        Client.RENDERER.textCentered("xxx", width / 2f, 40, TextureUse.SFMEDIUM, 6f, Color.LIGHT_GRAY);

        if (trackingTimer >= TRACKING_TIME_PER_CYCLE) {
            currentMode = Mode.FLICK;
            flicksInCurrentCycle = 0;
            trackingTimer = 0f;
            completedCycles++;
            activeCircle = null;
        }
    }

    private void renderSaveButton(int mouseX, int mouseY) {
        float btnW = 150, btnH = 28;
        float btnX = width - btnW - 20;
        float btnY = height - btnH - 20;

        boolean hover = MathUtility.mouseIn(btnX, btnY, btnW, btnH, mouseX, mouseY);
        Color btnCol = hover ? new Color(40, 200, 100) : new Color(30, 160, 80);

        Client.RENDERER.rect(btnX, btnY, btnW, btnH, new Vector4f(8), 1f, btnCol, btnCol, btnCol, btnCol);
        Client.RENDERER.textCentered("Stop and Save cfg", btnX + btnW / 2f, btnY + 10, TextureUse.SFMEDIUM, 6.5f, Color.WHITE);
    }

    private void spawnNewCircle(int startX, int startY) {
        float size = 18f + MathUtility.random(0f, 6f);
        float x = MathUtility.random(140f, width - 140f);
        float y = MathUtility.random(140f, height - 140f);

        activeCircle = new TargetCircle(x, y, size);
        currentFlick = new FlickTrack(startX, startY, x, y, size);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (!opened || button != 0 || isSaving) return false;

        if (currentMode == Mode.NAMING) {
            float cardW = 320, cardH = 170;
            float cardX = (width - cardW) / 2f;
            float cardY = (height - cardH) / 2f;
            float btnX = cardX + 25, btnY = cardY + 110, btnW = cardW - 50, btnH = 32;

            if (MathUtility.mouseIn(btnX, btnY, btnW, btnH, mouseX, mouseY) && !profileName.isEmpty()) {
                currentMode = Mode.FLICK;
                flicks.clear();
                return true;
            }
        } else {
            float btnW = 170, btnH = 34;
            float btnX = width - btnW - 20;
            float btnY = height - btnH - 20;

            if (MathUtility.mouseIn(btnX, btnY, btnW, btnH, mouseX, mouseY)) {
                isSaving = true;
                mc.execute(this::finishAndSave);
                return true;
            }

            if (currentMode == Mode.FLICK && activeCircle != null) {
                double dist = Math.hypot(mouseX - activeCircle.x, mouseY - activeCircle.y);
                if (dist <= activeCircle.size) {
                    if (currentFlick != null && currentFlick.samples.size() > 2) {
                        currentFlick.clickRelX = (float) ((mouseX - activeCircle.x) / activeCircle.size);
                        currentFlick.clickRelY = (float) ((activeCircle.y - mouseY) / activeCircle.size);
                        flicks.add(currentFlick);
                    }

                    flicksInCurrentCycle++;

                    if (flicksInCurrentCycle >= FLICKS_PER_CYCLE) {
                        currentMode = Mode.TRACKING;
                        trackingTimer = 0f;
                        movingTarget = new MovingPatternTarget(mouseX, mouseY);
                    } else {
                        spawnNewCircle(mouseX, mouseY);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private void finishAndSave() {
        if (flicks.isEmpty()) {
            setOpened(false);
            return;
        }

        float sumAvgSpeed = 0f, maxPeakSpeed = 0f;
        float sumYawSpeed = 0f, sumPitchSpeed = 0f;
        float sumAccelRatio = 0f, sumJitter = 0f, sumSway = 0f;
        float sumArcCurvature = 0f, sumOvershoot = 0f, sumDecelFriction = 0f;

        float[] tempPathArc = new float[10];

        for (FlickTrack f : flicks) {
            sumAvgSpeed += f.getAvgSpeed();
            sumYawSpeed += f.getAvgYawSpeed();
            sumPitchSpeed += f.getAvgPitchSpeed();

            if (f.getPeakSpeed() > maxPeakSpeed) maxPeakSpeed = f.getPeakSpeed();

            sumAccelRatio += f.getAccelRatio();
            sumJitter += f.getMicroJitter();
            sumSway += f.getMacroSway();
            sumArcCurvature += f.getArcCurvature();
            sumOvershoot += f.getOvershoot();
            sumDecelFriction += f.getDecelerationFriction();

            f.extract10PointArcCurve(tempPathArc);
        }

        int n = flicks.size();
        float avgSpd = sumAvgSpeed / n;
        float avgYawSpd = sumYawSpeed / n;
        float avgPitchSpd = sumPitchSpeed / n;

        float avgAccelRatio = sumAccelRatio / n;
        float avgJitter = sumJitter / n;
        float avgSway = sumSway / n;
        float avgArcCurvature = sumArcCurvature / n;
        float avgOvershoot = sumOvershoot / n;
        float avgFriction = sumDecelFriction / n;

        float sumTrackTremor = 0f;
        for (float tr : trackingTremors) sumTrackTremor += tr;
        float avgTrackingTremor = trackingTremors.isEmpty() ? 0.035f : (sumTrackTremor / trackingTremors.size()) * 0.005f;

        BuilderProfile profile = new BuilderProfile();
        profile.name = profileName;

        profile.avgSpeed = MathUtility.clamp(avgSpd * 0.65f, 0.25f, 0.90f);
        profile.maxSpeed = MathUtility.clamp(maxPeakSpeed * 18f, 12f, 35f);
        profile.yawPitchRatio = MathUtility.clamp(avgYawSpd / Math.max(0.001f, avgPitchSpd), 1.1f, 2.8f);
        profile.dynamicRatio = MathUtility.clamp(maxPeakSpeed / Math.max(0.01f, avgSpd), 1.2f, 2.8f);
        profile.accelRatio = MathUtility.clamp(avgAccelRatio, 0.20f, 0.60f);
        profile.inertia = MathUtility.clamp(0.85f - (avgSpd * 0.15f), 0.35f, 0.75f);
        profile.accelLimit = MathUtility.clamp(maxPeakSpeed * 2.2f, 1.5f, 5.0f);
        profile.kineticFriction = MathUtility.clamp(avgFriction, 0.50f, 0.85f);
        profile.submovementDamping = MathUtility.clamp(0.10f + (1.0f - avgFriction) * 0.18f, 0.08f, 0.35f);

        profile.arcCurvature = MathUtility.clamp(avgArcCurvature * 0.12f, 0.02f, 0.30f);
        profile.microJitter = MathUtility.clamp(avgJitter * 0.08f, 0.01f, 0.10f);
        profile.macroSway = MathUtility.clamp(avgSway * 0.06f, 0.01f, 0.12f);
        profile.overshootFactor = MathUtility.clamp(avgOvershoot * 0.10f, 0.02f, 0.30f);
        profile.trackingTremor = MathUtility.clamp(avgTrackingTremor, 0.01f, 0.08f);
        profile.screenShakeIntensity = MathUtility.clamp(avgJitter * 0.08f + avgTrackingTremor * 1.0f, 0.02f, 0.15f);
        profile.wobbleAmplitude = MathUtility.clamp(avgJitter * 0.04f + avgArcCurvature * 0.04f, 0.01f, 0.06f);

        for (int i = 0; i < 10; i++) {
            profile.pathArcCurve[i] = tempPathArc[i] / n;
        }

        extract15PointsInHumanZone(flicks, profile);

        profile.save();

        setOpened(false);
        ChatUtility.send("§a[NeuroBuilder v14] Profile '§e" + profileName + "§a' calibrated with exact hand shake & trajectory!");
    }

    private void extract15PointsInHumanZone(List<FlickTrack> list, BuilderProfile profile) {
        for (int i = 0; i < 15; i++) {
            if (i < list.size()) {
                FlickTrack f = list.get(i);
                profile.relativePointX[i] = MathUtility.clamp(f.clickRelX * 0.20f, -0.20f, 0.20f);
                profile.relativePointY[i] = MathUtility.clamp(0.60f + f.clickRelY * 0.12f, 0.48f, 0.72f);
            }
        }
    }

    private char getCharFromKey(int key) {
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) return (char) ('a' + (key - GLFW.GLFW_KEY_A));
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) return (char) ('0' + (key - GLFW.GLFW_KEY_0));
        if (key == GLFW.GLFW_KEY_MINUS) return '_';
        return 0;
    }

    public static class FlickTrack {
        public float startX, startY, targetX, targetY, targetSize;
        public float clickRelX = 0f, clickRelY = 0f;
        public List<PathSample> samples = new ArrayList<>();

        public FlickTrack(float startX, float startY, float targetX, float targetY, float targetSize) {
            this.startX = startX;
            this.startY = startY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.targetSize = targetSize;
        }

        public void addSample(float x, float y, float yawSpeed, float pitchSpeed, float speed) {
            samples.add(new PathSample(x, y, yawSpeed, pitchSpeed, speed));
        }

        public void extract10PointArcCurve(float[] arcOutput) {
            if (samples.size() < 3) return;
            float lineDist = (float) Math.hypot(targetX - startX, targetY - startY);
            if (lineDist < 0.001f) return;

            for (int i = 0; i < 10; i++) {
                float targetProgress = i / 9.0f;
                PathSample closestSample = null;
                float minProgressDiff = 999f;

                for (PathSample s : samples) {
                    float sampleDist = (float) Math.hypot(s.x - startX, s.y - startY);
                    float progress = sampleDist / lineDist;
                    float diff = Math.abs(progress - targetProgress);
                    if (diff < minProgressDiff) {
                        minProgressDiff = diff;
                        closestSample = s;
                    }
                }

                if (closestSample != null) {
                    float perpDev = ((targetY - startY) * closestSample.x - (targetX - startX) * closestSample.y + targetX * startY - targetY * startX) / lineDist;
                    arcOutput[i] += MathUtility.clamp(perpDev * 0.01f, -0.25f, 0.25f);
                }
            }
        }

        public float getAvgSpeed() {
            if (samples.isEmpty()) return 0f;
            float sum = 0f;
            for (PathSample s : samples) sum += s.speed;
            return sum / samples.size();
        }

        public float getAvgYawSpeed() {
            if (samples.isEmpty()) return 0f;
            float sum = 0f;
            for (PathSample s : samples) sum += s.yawSpeed;
            return sum / samples.size();
        }

        public float getAvgPitchSpeed() {
            if (samples.isEmpty()) return 0f;
            float sum = 0f;
            for (PathSample s : samples) sum += s.pitchSpeed;
            return sum / samples.size();
        }

        public float getPeakSpeed() {
            float max = 0f;
            for (PathSample s : samples) if (s.speed > max) max = s.speed;
            return max;
        }

        public float getAccelRatio() {
            if (samples.size() < 3) return 0.35f;
            float peak = getPeakSpeed();
            int peakIndex = 0;
            for (int i = 0; i < samples.size(); i++) {
                if (samples.get(i).speed >= peak) {
                    peakIndex = i;
                    break;
                }
            }
            return (float) peakIndex / (float) samples.size();
        }

        public float getDecelerationFriction() {
            if (samples.size() < 4) return 0.72f;
            int mid = samples.size() / 2;
            float firstHalfSpd = 0f, secondHalfSpd = 0f;
            for (int i = 0; i < mid; i++) firstHalfSpd += samples.get(i).speed;
            for (int i = mid; i < samples.size(); i++) secondHalfSpd += samples.get(i).speed;

            float ratio = secondHalfSpd / Math.max(0.001f, firstHalfSpd);
            return MathUtility.clamp(0.50f + ratio * 0.35f, 0.55f, 0.88f);
        }

        public float getOvershoot() {
            if (samples.size() < 4) return 0f;
            float maxDist = 0f;
            float targetDist = (float) Math.hypot(targetX - startX, targetY - startY);
            for (PathSample s : samples) {
                float d = (float) Math.hypot(s.x - startX, s.y - startY);
                if (d > maxDist) maxDist = d;
            }
            return Math.max(0f, maxDist - targetDist) / Math.max(1f, targetDist);
        }

        public float getArcCurvature() {
            if (samples.size() < 3) return 0f;
            PathSample mid = samples.get(samples.size() / 2);
            float lineDist = (float) Math.hypot(targetX - startX, targetY - startY);
            if (lineDist < 0.001f) return 0f;
            return Math.abs((targetY - startY) * mid.x - (targetX - startX) * mid.y + targetX * startY - targetY * startX) / lineDist;
        }

        public float getMicroJitter() {
            if (samples.size() < 3) return 0f;
            float totalJitter = 0f;
            for (int i = 1; i < samples.size() - 1; i++) {
                float prevSpd = samples.get(i - 1).speed;
                float currSpd = samples.get(i).speed;
                float nextSpd = samples.get(i + 1).speed;
                totalJitter += Math.abs(currSpd - (prevSpd + nextSpd) * 0.5f);
            }
            return totalJitter / samples.size();
        }

        public float getMacroSway() {
            if (samples.size() < 3) return 0f;
            float lineDist = (float) Math.hypot(targetX - startX, targetY - startY);
            if (lineDist < 0.001f) return 0f;

            float totalDev = 0f;
            for (PathSample p : samples) {
                float dev = Math.abs((targetY - startY) * p.x - (targetX - startX) * p.y + targetX * startY - targetY * startX) / lineDist;
                totalDev += dev;
            }
            return totalDev / samples.size();
        }
    }

    public static record PathSample(float x, float y, float yawSpeed, float pitchSpeed, float speed) {}

    public static class TargetCircle {
        public final float x, y, size;
        public float animProgress = 0f;

        public TargetCircle(float x, float y, float size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }

        public void update(float delta) {
            if (animProgress < 1.0f) {
                animProgress = Math.min(1.0f, animProgress + delta * 12f);
            }
        }
    }

    public static class MovingPatternTarget {
        public float x, y;
        private float timeElapsed = 0f;
        private float patternTimer = 0f;
        private int currentPattern = 0;

        private float vx = 210f, vy = 140f;
        private float orbitAngle = 0f;

        public MovingPatternTarget(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public void update(float delta, float boundsW, float boundsH) {
            timeElapsed += delta;
            patternTimer += delta;

            if (patternTimer >= 2.5f) {
                patternTimer = 0f;
                currentPattern = (currentPattern + 1) % 4;
            }

            switch (currentPattern) {
                case 0 -> {
                    if (MathUtility.random(0f, 100f) < 5f) vx *= -1;
                    x += vx * delta * 1.3f;
                    y += (float) Math.sin(timeElapsed * 10f) * 40f * delta;
                }
                case 1 -> {
                    x += vx * delta;
                    y += (float) Math.cos(timeElapsed * 5f) * 180f * delta;
                }
                case 2 -> {
                    orbitAngle += delta * 3.5f;
                    x += (float) Math.cos(orbitAngle) * 180f * delta;
                    y += (float) Math.sin(orbitAngle) * 180f * delta;
                }
                case 3 -> {
                    float burstMult = (float) Math.sin(timeElapsed * 8f) > 0.2f ? 1.8f : 0.4f;
                    x += vx * delta * burstMult;
                    y += vy * delta * burstMult;
                }
            }

            if (x < 150 || x > boundsW - 150) vx *= -1;
            if (y < 150 || y > boundsH - 150) vy *= -1;
            x = MathUtility.clamp(x, 150f, boundsW - 150f);
            y = MathUtility.clamp(y, 150f, boundsH - 150f);
        }

        public String getPatternName() {
            return switch (currentPattern) {
                case 0 -> "A-D Strafe Jitter";
                case 1 -> "Sine Wave Motion";
                case 2 -> "Orbital Circle";
                case 3 -> "Erratic Speed Burst";
                default -> "Unknown";
            };
        }
    }
}