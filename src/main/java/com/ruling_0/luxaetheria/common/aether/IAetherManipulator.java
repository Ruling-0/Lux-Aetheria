package com.ruling_0.luxaetheria.common.aether;


/**
 * An Interface for things which intake and/or output Aether.
 */
public interface IAetherManipulator {
    boolean addSource(IAetherManipulator source);

    boolean removeSource(IAetherManipulator source);

    IAetherManipulator getSource();

    boolean addSink(IAetherManipulator sink);

    boolean removeSink(IAetherManipulator sink);

    IAetherManipulator getSink();
}
