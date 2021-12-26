package it.unibo.effects.prova;

import it.unibo.alchemist.boundary.gui.effects.EffectSerializationFactory;

import java.io.File;
import java.io.IOException;

public class Test {
    public static void main(String[] args) throws IOException {
        var prova = new it.unibo.alchemist.boundary.gui.effects.DrawBidimensionalGaussianLayersGradient();
        EffectSerializationFactory.effectToFile(new File("extracted.json"), prova);
    }
}
