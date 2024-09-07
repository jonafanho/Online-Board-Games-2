import {Component} from "@angular/core";
import {ClientState, Request, Stage} from "../../../entity/generated/bluff_elimination";
import {DataService} from "../../../service/data.service";

@Component({
	selector: "game-bluff-elimination",
	standalone: true,
	imports: [],
	templateUrl: "./bluff-elimination.component.html",
	styleUrl: "./bluff-elimination.component.css",
})
export class BluffEliminationComponent {

	constructor(private readonly dataService: DataService) {
	}

	getState() {
		const clientState = this.dataService.getRoom()?.getState<ClientState, Stage>();
		return clientState ? clientState.stage : "";
	}

	private sendUpdate() {
		this.dataService.sendUpdate<Request>({});
	}
}
