package gg.topchdlc.vse.utils.player;

import gg.topchdlc.MinecraftHolder;
import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class Pathfinder implements MinecraftHolder {

    private static class Node {
        BlockPos pos;
        Node parent;
        double g;
        double h;

        Node(BlockPos pos, Node parent, double g, double h) {
            this.pos = pos;
            this.parent = parent;
            this.g = g;
            this.h = h;
        }

        double getF() {
            return g + h;
        }
    }

    public static List<BlockPos> findPath(BlockPos start, BlockPos end) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(Node::getF));
        Set<BlockPos> closedSet = new HashSet<>();

        openSet.add(new Node(start, null, 0, start.getManhattanDistance(end)));

        int iterations = 0;
        while (!openSet.isEmpty() && iterations < 1500) {
            iterations++;
            Node current = openSet.poll();
            closedSet.add(current.pos);

            if (isAdjacent(current.pos, end)) {
                List<BlockPos> path = new ArrayList<>();
                Node curr = current;
                while (curr != null) {
                    path.add(0, curr.pos);
                    curr = curr.parent;
                }
                return path;
            }

            for (BlockPos neighbor : getValidNeighbors(current.pos)) {
                if (closedSet.contains(neighbor)) continue;

                double moveCost = (current.pos.getY() != neighbor.getY()) ? 1.5 : 1.0;
                double tentativeG = current.g + moveCost;

                Node existing = findInQueue(openSet, neighbor);

                if (existing == null) {
                    openSet.add(new Node(neighbor, current, tentativeG, neighbor.getManhattanDistance(end)));
                } else if (tentativeG < existing.g) {
                    openSet.remove(existing);
                    openSet.add(new Node(neighbor, current, tentativeG, neighbor.getManhattanDistance(end)));
                }
            }
        }
        return null;
    }

    private static boolean isAdjacent(BlockPos playerPos, BlockPos chestPos) {
        int dx = Math.abs(playerPos.getX() - chestPos.getX());
        int dy = Math.abs(playerPos.getY() - chestPos.getY());
        int dz = Math.abs(playerPos.getZ() - chestPos.getZ());

        return ((dx == 1 && dz == 0) || (dx == 0 && dz == 1)) && dy <= 1;
    }

    private static List<BlockPos> getValidNeighbors(BlockPos current) {
        List<BlockPos> list = new ArrayList<>();
        int[] dx = {1, -1, 0, 0, 1, -1, 1, -1};
        int[] dz = {0, 0, 1, -1, 1, 1, -1, -1};

        boolean currentIsClimbable = isClimbable(current);

        for (int i = 0; i < 8; i++) {
            BlockPos adj = current.add(dx[i], 0, dz[i]);

            if (i >= 4) {
                BlockPos corner1 = current.add(dx[i], 0, 0);
                BlockPos corner2 = current.add(0, 0, dz[i]);
                if (!isPassable(corner1) || !isPassable(corner1.up()) ||
                        !isPassable(corner2) || !isPassable(corner2.up())) {
                    continue;
                }
            }

            if (canStandAt(adj)) {
                list.add(adj);
                continue;
            }

            if (isClimbable(adj) && isPassable(adj.up())) {
                list.add(adj);
                continue;
            }

            BlockPos up = adj.up();
            if (canStandAt(up)) {
                if (isPassable(current.up().up())) {
                    list.add(up);
                }
                continue;
            }

            for (int drop = 1; drop <= 3; drop++) {
                BlockPos down = adj.down(drop);
                if (canStandAt(down)) {
                    boolean clear = true;
                    for (int h = 0; h < drop; h++) {
                        if (!isPassable(adj.down(h)) || !isPassable(adj.down(h).up())) {
                            clear = false;
                            break;
                        }
                    }
                    if (clear) {
                        list.add(down);
                    }
                    break;
                }
            }
        }

        if (currentIsClimbable) {
            BlockPos climbUp = current.up();
            if (isClimbable(climbUp) && isPassable(climbUp.up())) {
                list.add(climbUp);
            }
            BlockPos climbDown = current.down();
            if ((isClimbable(climbDown) || canStandAt(climbDown)) && isPassable(climbDown.up())) {
                list.add(climbDown);
            }
        }

        return list;
    }

    private static Node findInQueue(PriorityQueue<Node> queue, BlockPos pos) {
        for (Node node : queue) {
            if (node.pos.equals(pos)) return node;
        }
        return null;
    }

    public static boolean isClimbable(BlockPos pos) {
        if (mc.world == null) return false;
        BlockState state = mc.world.getBlockState(pos);
        return state.getBlock() instanceof LadderBlock || state.getBlock() instanceof VineBlock;
    }

    public static boolean isPassable(BlockPos pos) {
        if (mc.world == null) return true;
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) return true;

        Block block = state.getBlock();

        if (block instanceof CarpetBlock ||
                block instanceof SculkVeinBlock ||
                block instanceof ButtonBlock ||
                block instanceof LeverBlock ||
                block instanceof LightBlock) {
            return true;
        }

        if (block instanceof FenceBlock || block instanceof WallBlock || block instanceof FenceGateBlock) {
            return false;
        }

        return !state.blocksMovement() || state.getCollisionShape(mc.world, pos).isEmpty();
    }

    public static boolean canStandAt(BlockPos pos) {
        if (!isPassable(pos)) return false;
        if (!isPassable(pos.up())) return false;

        BlockPos below = pos.down();
        BlockState ground = mc.world.getBlockState(below);

        if (ground.getBlock() instanceof CarpetBlock || ground.getBlock() instanceof SculkVeinBlock) {
            below = below.down();
            ground = mc.world.getBlockState(below);
        }

        if (ground.getBlock() instanceof FenceBlock ||
                ground.getBlock() instanceof WallBlock ||
                ground.getBlock() instanceof FenceGateBlock) {
            return false;
        }

        return !ground.isAir() && (ground.blocksMovement() ||
                !ground.getCollisionShape(mc.world, below).isEmpty() ||
                ground.getBlock() instanceof SlabBlock ||
                ground.getBlock() instanceof StairsBlock);
    }
}