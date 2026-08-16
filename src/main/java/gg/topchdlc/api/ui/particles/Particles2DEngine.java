package gg.topchdlc.api.ui.particles;


import org.joml.Vector2f;

import java.util.ArrayList;

public class Particles2DEngine {
    ArrayList<Particle2D> particles = new ArrayList<>();
    private static final int MAX_PARTICLES = 80;

    public void render() {
        for (Particle2D p : particles) {
            p.render(0, 0);
        }
        particles.removeIf(Particle2D::removed);
    }
    public void addParticle(Particle2D p) {
        if (particles.size() >= MAX_PARTICLES) return;
        particles.add(p);
    }
    public void addParticle(Vector2f pos, Vector2f vel, float dir, float roll, long alive) {
        if (particles.size() >= MAX_PARTICLES) return;
        particles.add(new Particle2D(pos, vel, dir, roll, alive));
    }
}
