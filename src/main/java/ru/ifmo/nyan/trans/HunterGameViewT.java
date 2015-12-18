package ru.ifmo.nyan.trans;

import ru.ifmo.nyan.hunter.HunterGame;

import java.util.List;
import java.util.stream.Collectors;

public class HunterGameViewT {
    public final HunterInstanceT hunter;
    public final List<HunterInstanceT> shadows;

    public HunterGameViewT(HunterGame.HunterGameView view) {
        this.hunter = new HunterInstanceT(view.hunter());
        this.shadows = view.shadows().map(HunterInstanceT::new)
                .collect(Collectors.toList());
    }
}
