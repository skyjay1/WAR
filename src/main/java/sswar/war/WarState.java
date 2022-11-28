package sswar.war;

import com.google.common.collect.ImmutableMap;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum WarState implements StringRepresentable {
    CREATING((byte) 0, "creating"),
    RECRUITING((byte) 1, "recruiting"),
    PREPARING((byte) 2, "preparing"),
    ACTIVE((byte) 3, "active"),
    ENDED((byte) 4, "ended"),
    INVALID((byte) 5, "invalid");

    private static final Map<String, WarState> STRING_TO_STATE_MAP = ImmutableMap.copyOf(Arrays.stream(values())
            .collect(Collectors.<WarState, String, WarState>toMap(WarState::getSerializedName, Function.identity())));
    private static final Map<Byte, WarState> ID_TO_STATE_MAP = ImmutableMap.copyOf(Arrays.stream(values())
            .collect(Collectors.<WarState, Byte, WarState>toMap(WarState::getId, Function.identity())));

    private final byte id;
    private final String name;
    private final String translationKey;

    WarState(final byte id, final String name) {
        this.id = id;
        this.name = name;
        this.translationKey = "war.warstate." + name;
    }

    public static WarState getById(final byte id) {
        return ID_TO_STATE_MAP.getOrDefault(id, INVALID);
    }

    public static WarState getByName(final String name) {
        return STRING_TO_STATE_MAP.getOrDefault(name, INVALID);
    }

    public byte getId() {
        return this.id;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }
}
