package gg.topchdlc.api.ui.particles;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.RendererObject;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.MathUtility;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.util.Identifier;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;

@Slf4j
public class Particle2D extends RendererObject {
    private static final Identifier GLOW_TEXTURE = Identifier.of("topchdlc", "images/world/ghost-glow.png");
    public Vector2f vel;
    public float dir;
    public float roll;
    public long alive;
    public long deleteIn;
    public Particle2D(Vector2f pos, Vector2f vel, float dir, float roll, long alive) {
        this.x = pos.x;
        this.y = pos.y;
        this.vel = vel;
        this.dir = dir;
        this.roll = roll;
        this.alive = alive;
        this.deleteIn = System.currentTimeMillis() + alive;
    }

    public boolean removed() {
        return System.currentTimeMillis() > deleteIn;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        dir += roll;
        x += (float) (Math.sin(dir * MathUtility.TO_RADIANS) * vel.x);
        y += (float) (Math.cos(dir * MathUtility.TO_RADIANS) * vel.y);
        CRenderSystem system = Client.RENDERER.getCrenderSystem();
        float alpha = system.alpha();
        float f = Math.min(255, deleteIn - System.currentTimeMillis()) / 255F;
        system.alpha(f * alpha);
        Color c = ClientSettings.INSTANCE.getColor((int) x * 4);
        Client.RENDERER.textureRaw(GLOW_TEXTURE, x - 5, y - 5, 10, 10, 1, new Vector4f(0), c, c, c, c);
        system.alpha(alpha);
    }
}
