package gg.topchdlc.vse.utils.animations;


import gg.topchdlc.vse.utils.math.TimeUtility;
import lombok.Getter;
import lombok.Setter;


public abstract class OldAnimation {

    public TimeUtility timerUtil = new TimeUtility();
    @Setter
    @Getter
    protected int duration;
    @Setter
    @Getter
    protected double endPoint;
    @Getter
    protected Direction direction;

    public OldAnimation(int ms, double endPoint) {
        this.duration = ms;
        this.endPoint = endPoint;
        this.direction = Direction.FORWARDS;
    }


    public OldAnimation(int ms, double endPoint, Direction direction) {
        this.duration = ms;
        this.endPoint = endPoint;
        this.direction = direction;  }

    public boolean finished(Direction direction) {
        return isDone() && this.direction.equals(direction);
    }

    public double getLinearOutput() {
        return 1 - ((timerUtil.getTime() / (double) duration) * endPoint);
    }

    public void reset() {
        timerUtil.reset();
    }

    public boolean isDone() {
        return timerUtil.reached(duration);
    }

    public void changeDirection() {
        setDirection(direction.opposite());
    }

    public void setDirection(Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
            timerUtil.setTime(System.currentTimeMillis() - (duration - Math.min(duration, timerUtil.getTime())));
        }
    }

    protected boolean correctOutput() {
        return false;
    }

    public float getOutput() {
        if (direction == Direction.FORWARDS) {
            if (isDone())
                return (float) endPoint;
            return (float) (getEquation(timerUtil.getTime()) * endPoint);
        } else {
            if (isDone()) return 0;
            if (correctOutput()) {
                double revTime = Math.min(duration, Math.max(0, duration - timerUtil.getTime()));
                return (float) (getEquation(revTime) * endPoint);
            } else return (float) ((1 - getEquation(timerUtil.getTime())) * endPoint);
        }
    }



    protected abstract double getEquation(double x);

}
