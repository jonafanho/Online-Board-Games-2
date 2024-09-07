import {Stomp, StompSubscription} from "@stomp/stompjs";
import {NavigationEnd, Router} from "@angular/router";
import {DataService} from "../service/data.service";

export class Socket {
	private stompSubscription?: StompSubscription;

	public constructor(host: string, requestRoom: (roomCode: string) => void, clearRoom: () => void, dataService: DataService, router: Router) {
		const client = Stomp.client(`ws://${host}/socket`);
		client.onWebSocketError = error => console.error("WebSocket error!", error);
		client.onStompError = frame => console.error("Broker error!", frame);
		client.reconnectDelay = 2000;
		client.activate();

		const checkSubscriptions = () => {
			if (this.stompSubscription) {
				this.stompSubscription.unsubscribe();
				this.stompSubscription = undefined;
			}

			const getRoomUpdate = (roomCode: string) => requestRoom(roomCode);
			const pathSplit = router.url.split("/");
			const roomCode = pathSplit[pathSplit.length - 1];

			if (roomCode) {
				this.stompSubscription = client.subscribe(`/topic/${roomCode}`, ({body}) => {
					if ((JSON.parse(body) as { sender: string }).sender != dataService.getPlayer()?.uuid) {
						getRoomUpdate(roomCode);
					}
				});

				if (roomCode != dataService.getRoom()?.code) {
					getRoomUpdate(roomCode);
				}
			} else {
				clearRoom();
			}
		};

		client.onConnect = () => checkSubscriptions();
		router.events.subscribe((event) => {
			if (event instanceof NavigationEnd) {
				checkSubscriptions();
			}
		});
	}
}
