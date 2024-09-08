import {Component} from "@angular/core";
import {ClientState, Request, Stage} from "../../../../entity/generated/bluff_elimination";
import {DataService} from "../../../../service/data.service";
import {MatRadioModule} from "@angular/material/radio";
import {MatButtonModule} from "@angular/material/button";
import {MatCheckboxModule} from "@angular/material/checkbox";
import {ReactiveFormsModule} from "@angular/forms";
import {MatIconModule} from "@angular/material/icon";
import {MatCardModule} from "@angular/material/card";

@Component({
	selector: "game-bluff-elimination",
	standalone: true,
	imports: [
		MatRadioModule,
		MatButtonModule,
		MatCheckboxModule,
		ReactiveFormsModule,
		MatIconModule,
		MatCardModule,
	],
	templateUrl: "./bluff-elimination.component.html",
	styleUrl: "./bluff-elimination.component.css",
})
export class BluffEliminationComponent {

	constructor(private readonly dataService: DataService) {
	}

	private sendUpdate(request: Request) {
		this.dataService.sendUpdate<Request>(request);
	}

	private getState() {
		return this.dataService.getRoom()?.getState<ClientState, Stage>();
	}
}
