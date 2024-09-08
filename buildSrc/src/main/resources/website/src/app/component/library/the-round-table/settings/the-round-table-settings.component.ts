import {Component} from "@angular/core";
import {ClientState, Request, Stage} from "../../../../entity/generated/the_round_table";
import {DataService} from "../../../../service/data.service";

@Component({
	selector: "game-settings-the-round-table",
	standalone: true,
	imports: [],
	templateUrl: "./the-round-table-settings.component.html",
	styleUrl: "./the-round-table-settings.component.css",
})
export class TheRoundTableSettingsComponent {

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
