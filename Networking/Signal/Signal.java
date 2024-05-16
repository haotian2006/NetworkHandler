package Networking.Signal;

import java.util.LinkedList ;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.locks.*;

public class Signal<T> {
	protected class SignalConnection implements Connection {
		private Consumer<T> callBack;
		private Signal<T> parent;
		
		SignalConnection(Signal<T> parent, Consumer<T> callBack) {
			this.parent = parent;
			this.callBack = callBack;
		}
		
		private void invoke(T value) {
			callBack.accept(value);
		}
		
		public void disconnect() {
			parent.disconnectConnection(this);
		}

	}

	
	protected class SignalEvent implements Event<T> {
		private Signal<T> parent;
		
		SignalEvent(Signal<T> parent) {
			this.parent = parent;		
		}
		
		public SignalConnection connect(Consumer<T> callBack) {
			return parent.connect(callBack);
		}
		public T await() {
			return parent.await();
		}

		@SuppressWarnings("unchecked")
		public SignalConnection once(Consumer<T> callBack) {
			Connection[] connectionLocal = new Connection[1];
			//Annoying way of getting this to work
			connectionLocal[0] = parent.connect((value) -> {
				callBack.accept(value);
				connectionLocal[0].disconnect(); 
			});
			return (SignalConnection)connectionLocal[0];
		}
	}
	
	

	private List<SignalConnection> connections;
	public Event<T> event;
	private Lock lock;
	private Condition await;
	private T value;

	public Signal(){
		lock = new ReentrantLock();
		await = lock.newCondition();
		connections = new LinkedList<SignalConnection>();
		event = new SignalEvent(this);
	}
	
	public SignalConnection connect(Consumer<T> callBack) {
		SignalConnection connection = new SignalConnection(this,callBack);
		connections.add(connection);
		return connection;
	}

	public T await() {
		lock.lock();
		try {
			await.await();
			return value;
		} catch (InterruptedException e) {
			System.out.println("Signal await interrupted");
			return null;
		} finally {
			lock.unlock();
		}
	}
	
	public void fire(T value) {
		this.value = value;
		lock.lock();
		try {
			this.value = value;
			await.signalAll();
		} finally {
			lock.unlock();
		}
		for(SignalConnection connection : connections)
			connection.invoke(value);
	}
	
	public void disconnectAll() {
		connections.clear();
	}



    private void disconnectConnection(SignalConnection connection ) {
		connections.clear();
	}

	
	@Override
	protected void finalize() throws Throwable {
		disconnectAll();
	}
	
}



