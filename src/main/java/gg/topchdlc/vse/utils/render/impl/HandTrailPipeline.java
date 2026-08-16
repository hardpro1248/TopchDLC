package gg.topchdlc.vse.utils.render.impl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.gl.GpuSampler;
import gg.topchdlc.api.render.system.ClientPipelines;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Created by daun kvass
 */
public class HandTrailPipeline {
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f(0f, 0f, 0f);
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final int BUFFER_SIZE = 80;

    private GpuBuffer uniformBuffer;
    private GpuBuffer dummyVertexBuffer;
    private GpuSampler linearSampler;
    private ByteBuffer dataBuffer;
    private GpuTexture[] textures;
    private GpuTextureView[] views;
    private int readIndex;
    private int width;
    private int height;
    private boolean initialized;

    public GpuTextureView update(GpuTextureView scene, GpuTextureView mask, int w, int h, float time, float intensity, float r, float g, float b, float a, float radius, float speed, float flameHeight, float softness, float blurRadius, float smoke, float activity, float slash, float slashDirection, float trailFade) {
        if (scene == null || mask == null || w <= 0 || h <= 0) return null;

        ensureInitialized();
        ensureFramebuffers(w, h);

        int writeIndex = 1 - readIndex;
        prepareUniformData(w, h, time, intensity, r, g, b, a, radius, speed, flameHeight, softness, blurRadius, smoke, activity, slash, slashDirection, trailFade);

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        uploadUniform(encoder);

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().write(
                RenderSystem.getModelViewMatrix(),
                COLOR_MODULATOR,
                MODEL_OFFSET,
                TEXTURE_MATRIX
        );

        try (RenderPass renderPass = encoder.createRenderPass(() -> "topchdlc:hand_trail", views[writeIndex], OptionalInt.of(0x00000000))) {
            renderPass.setPipeline(ClientPipelines.HAND_TRAIL_PIPELINE);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);
            renderPass.bindTexture("PrevTrailSampler", views[readIndex], linearSampler);
            renderPass.bindTexture("SceneSampler", scene, linearSampler);
            renderPass.bindTexture("MaskSampler", mask, linearSampler);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setUniform("HandTrailData", uniformBuffer);
            renderPass.draw(0, 6);
        }

        readIndex = writeIndex;
        return views[readIndex];
    }

    private void ensureInitialized() {
        if (initialized) return;

        dataBuffer = MemoryUtil.memAlloc(BUFFER_SIZE);
        dummyVertexBuffer = createDummyVertexBuffer("topchdlc:hand_trail_dummy_vertex");

        linearSampler = RenderSystem.getDevice().createSampler(
                AddressMode.CLAMP_TO_EDGE,
                AddressMode.CLAMP_TO_EDGE,
                FilterMode.LINEAR,
                FilterMode.LINEAR,
                1,
                OptionalDouble.empty()
        );

        textures = new GpuTexture[2];
        views = new GpuTextureView[2];
        initialized = true;
    }

    private void ensureFramebuffers(int w, int h) {
        if (width == w && height == h && textures[0] != null && textures[1] != null) return;

        cleanupFramebuffers();
        for (int i = 0; i < 2; i++) {
            final int index = i;
            textures[i] = RenderSystem.getDevice().createTexture(
                    () -> "topchdlc:hand_trail_" + index,
                    GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                    TextureFormat.RGBA8,
                    w,
                    h,
                    1,
                    1
            );
            views[i] = RenderSystem.getDevice().createTextureView(textures[i]);
        }
        width = w;
        height = h;
        readIndex = 0;
        clearFramebuffers();
    }

    public void clear() {
        if (views == null || views[0] == null || views[1] == null) return;
        clearFramebuffers();
    }

    private void clearFramebuffers() {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        for (int i = 0; i < 2; i++) {
            final int index = i;
            try (RenderPass ignored = encoder.createRenderPass(() -> "topchdlc:hand_trail_clear_" + index, views[i], OptionalInt.of(0x00000000))) {
            }
        }
    }

    private void prepareUniformData(int w, int h, float time, float intensity, float r, float g, float b, float a, float radius, float speed, float flameHeight, float softness, float blurRadius, float smoke, float activity, float slash, float slashDirection, float trailFade) {
        dataBuffer.clear();
        dataBuffer.putFloat(w);
        dataBuffer.putFloat(h);
        dataBuffer.putFloat(time);
        dataBuffer.putFloat(intensity);
        dataBuffer.putFloat(r);
        dataBuffer.putFloat(g);
        dataBuffer.putFloat(b);
        dataBuffer.putFloat(a);
        dataBuffer.putFloat(radius);
        dataBuffer.putFloat(speed);
        dataBuffer.putFloat(flameHeight);
        dataBuffer.putFloat(softness);
        dataBuffer.putFloat(blurRadius);
        dataBuffer.putFloat(smoke);
        dataBuffer.putFloat(activity);
        dataBuffer.putFloat(trailFade);
        dataBuffer.putFloat(slash);
        dataBuffer.putFloat(slashDirection);
        dataBuffer.putFloat(0.0f);
        dataBuffer.putFloat(0.0f);
        dataBuffer.flip();
    }

    private void uploadUniform(CommandEncoder encoder) {
        int size = dataBuffer.remaining();
        if (uniformBuffer == null || uniformBuffer.size() < size) {
            if (uniformBuffer != null) uniformBuffer.close();
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "topchdlc:hand_trail_uniform",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    size
            );
        }
        encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);
    }

    private void cleanupFramebuffers() {
        if (views != null) {
            for (int i = 0; i < views.length; i++) {
                if (views[i] != null) {
                    views[i].close();
                    views[i] = null;
                }
                if (textures[i] != null) {
                    textures[i].close();
                    textures[i] = null;
                }
            }
        }
        width = 0;
        height = 0;
    }

    private static GpuBuffer createDummyVertexBuffer(String name) {
        ByteBuffer dummyData = MemoryUtil.memAlloc(4);
        try {
            dummyData.putInt(0);
            dummyData.flip();
            return RenderSystem.getDevice().createBuffer(() -> name, GpuBuffer.USAGE_VERTEX, dummyData);
        } finally {
            MemoryUtil.memFree(dummyData);
        }
    }

    public GpuTexture texture() {
        return textures == null ? null : textures[readIndex];
    }

    public void close() {
        cleanupFramebuffers();
        if (linearSampler != null) {
            linearSampler.close();
            linearSampler = null;
        }
        if (uniformBuffer != null) {
            uniformBuffer.close();
            uniformBuffer = null;
        }
        if (dummyVertexBuffer != null) {
            dummyVertexBuffer.close();
            dummyVertexBuffer = null;
        }
        if (dataBuffer != null) {
            MemoryUtil.memFree(dataBuffer);
            dataBuffer = null;
        }
        initialized = false;
    }
}