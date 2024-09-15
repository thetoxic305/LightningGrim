package ac.grim.grimac.utils.collisions.blocks.connecting;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.datatypes.*;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.enums.*;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

public class DynamicHitboxPane extends DynamicConnecting implements CollisionFactory {

    private static final CollisionBox[] COLLISION_BOXES = makeShapes(1.0F, 1.0F, 16.0F, 0.0F, 16.0F, true);

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockState block, int x, int y, int z) {
        boolean east, north, south, west;

        // 1.13+ servers on 1.13+ clients send the full fence data
        if (isModernVersion(version)) {
            east = block.getEast() != East.FALSE;
            north = block.getNorth() != North.FALSE;
            south = block.getSouth() != South.FALSE;
            west = block.getWest() != West.FALSE;
        } else {
            east = connectsTo(player, version, x, y, z, BlockFace.EAST);
            north = connectsTo(player, version, x, y, z, BlockFace.NORTH);
            south = connectsTo(player, version, x, y, z, BlockFace.SOUTH);
            west = connectsTo(player, version, x, y, z, BlockFace.WEST);
        }

        // On 1.7 and 1.8 clients, and 1.13+ clients on 1.7 and 1.8 servers, the glass pane is + instead of |
        if (shouldUseOldPaneShape(version, north, south, east, west)) {
            north = south = east = west = true;
        }

        return version.isNewerThanOrEquals(ClientVersion.V_1_9)
                ? getModernCollisionBox(north, east, south, west)
                : getLegacyCollisionBox(north, east, south, west);
    }

    private boolean isModernVersion(ClientVersion version) {
        return PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13)
                && version.isNewerThanOrEquals(ClientVersion.V_1_13);
    }

    private boolean shouldUseOldPaneShape(ClientVersion version, boolean north, boolean south, boolean east, boolean west) {
        return (!north && !south && !east && !west) &&
                (version.isOlderThanOrEquals(ClientVersion.V_1_8) ||
                        (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8_8) &&
                                version.isNewerThanOrEquals(ClientVersion.V_1_13)));
    }

    private CollisionBox getModernCollisionBox(boolean north, boolean east, boolean south, boolean west) {
        return COLLISION_BOXES[getAABBIndex(north, east, south, west)].copy();
    }

    private CollisionBox getLegacyCollisionBox(boolean north, boolean east, boolean south, boolean west) {
        ComplexCollisionBox boxes = new ComplexCollisionBox();

        if ((!west || !east) && (west || east || north || south)) {
            if (west) {
                boxes.add(new SimpleCollisionBox(0.0F, 0.0F, 0.4375F, 0.5F, 1.0F, 0.5625F));
            } else if (east) {
                boxes.add(new SimpleCollisionBox(0.5F, 0.0F, 0.4375F, 1.0F, 1.0F, 0.5625F));
            }
        } else {
            boxes.add(new SimpleCollisionBox(0.0F, 0.0F, 0.4375F, 1.0F, 1.0F, 0.5625F));
        }

        if ((!north || !south) && (west || east || north || south)) {
            if (north) {
                boxes.add(new SimpleCollisionBox(0.4375F, 0.0F, 0.0F, 0.5625F, 1.0F, 0.5F));
            } else if (south) {
                boxes.add(new SimpleCollisionBox(0.4375F, 0.0F, 0.5F, 0.5625F, 1.0F, 1.0F));
            }
        } else {
            boxes.add(new SimpleCollisionBox(0.4375F, 0.0F, 0.0F, 0.5625F, 1.0F, 1.0F));
        }

        return boxes;
    }

    @Override
    public boolean canConnectToGlassBlock() {
        return true;
    }

    @Override
    public boolean checkCanConnect(GrimPlayer player, WrappedBlockState state, StateType one, StateType two, BlockFace direction) {
        if (BlockTags.GLASS_PANES.contains(one) || one == StateTypes.IRON_BARS) {
            return true;
        } else {
            return CollisionData.getData(one)
                    .getMovementCollisionBox(player, player.getClientVersion(), state, 0, 0, 0)
                    .isSideFullBlock(direction);
        }
    }
}