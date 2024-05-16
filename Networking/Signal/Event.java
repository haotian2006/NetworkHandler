package Networking.Signal;

import java.util.function.Consumer;
public interface Event<T> {
    public Connection connect(Consumer<T> callBack);
    public T await() ;
    public Connection once(Consumer<T> callBack);
}


