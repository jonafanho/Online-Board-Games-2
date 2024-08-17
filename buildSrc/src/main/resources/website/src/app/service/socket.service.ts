import {Injectable} from "@angular/core";
import {Stomp} from "@stomp/stompjs";

@Injectable({providedIn: "root"})
export class SocketService {
	private stompClient = Stomp.client("ws://localhost:8080/gs-guide-websocket");

	constructor() {
		this.stompClient.onConnect = frame => {
			this.setConnected(true);
		};

		this.stompClient.onDisconnect = frame => {
			this.setConnected(false);
		};

		this.stompClient.onWebSocketError = error => console.error("WebSocket error!", error);
		this.stompClient.onStompError = frame => console.error("Broker error!", frame.headers["message"], frame.body);
	}

	private setConnected(connected: boolean) {

	}

	public connect() {
		this.stompClient.activate();
	}

	public disconnect() {
		this.stompClient.deactivate().then();
	}
}
