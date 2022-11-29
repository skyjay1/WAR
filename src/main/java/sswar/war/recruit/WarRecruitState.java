package sswar.war.recruit;

import com.google.common.collect.ImmutableMap;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum WarRecruitState implements StringRepresentable {
    PENDING((byte)0, "pending"),
    ACCEPT((byte)1, "accept"),
    DENY((byte)2, "deny"),
    INVALID((byte)3, "invalid");

    private static final Map<Byte, WarRecruitState> ID_TO_STATE_MAP = ImmutableMap.copyOf(Arrays.stream(values())
            .collect(Collectors.<WarRecruitState, Byte, WarRecruitState>toMap(WarRecruitState::getId, Function.identity())));

    private final byte id;
    private final String name;

    WarRecruitState(final byte id, final String name) {
        this.id = id;
        this.name = name;
    }

    public static WarRecruitState getById(final byte id) {
        return ID_TO_STATE_MAP.getOrDefault(id, INVALID);
    }

    public byte getId() {
        return this.id;
    }

    public boolean isAccepted() {
        return this == ACCEPT;
    }

    public boolean isRejected() {
        return this == DENY || this == INVALID;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
