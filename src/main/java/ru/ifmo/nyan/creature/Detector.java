package ru.ifmo.nyan.creature;

import org.jetbrains.annotations.Nullable;
import ru.ifmo.nyan.common.Direction;

public interface Detector extends Creature {
    @Nullable
    Direction getLastDirection();
}
