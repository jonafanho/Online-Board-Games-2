import {Component} from "@angular/core";
import {CharacterSet, ClientState, Request, Stage} from "../../../../entity/generated/bluff_elimination";
import {DataService} from "../../../../service/data.service";
import {MatRadioModule} from "@angular/material/radio";
import {MatButtonModule} from "@angular/material/button";
import {MatCheckboxModule} from "@angular/material/checkbox";
import {FormControl, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatInputModule} from "@angular/material/input";

@Component({
	selector: "game-settings-bluff-elimination",
	standalone: true,
	imports: [
		MatRadioModule,
		MatButtonModule,
		MatCheckboxModule,
		ReactiveFormsModule,
		MatFormFieldModule,
		MatInputModule,
	],
	templateUrl: "./bluff-elimination-settings.component.html",
	styleUrl: "./bluff-elimination-settings.component.css",
})
export class BluffEliminationSettingsComponent {
	protected readonly CharacterSet = CharacterSet;
	protected readonly settingsFormGroup = new FormGroup({
		characterSet: new FormControl(CharacterSet.WithAmbassador),
		startingCardsPerPlayer: new FormControl(2),
		startingCoinsPerPlayer: new FormControl(2),
		enableTeams: new FormControl(false),
		revealCharacterOnElimination: new FormControl(false),
	});

	constructor(private readonly dataService: DataService) {
		const onRoomUpdate = () => {
			const state = this.dataService.getRoom()?.getState<ClientState, Stage>();
			if (state) {
				this.settingsFormGroup.controls.characterSet.setValue(state.characterSet);
				this.settingsFormGroup.controls.startingCardsPerPlayer.setValue(state.startingCardsPerPlayer);
				this.settingsFormGroup.controls.startingCoinsPerPlayer.setValue(state.startingCoinsPerPlayer);
				this.settingsFormGroup.controls.enableTeams.setValue(state.enableTeams);
				this.settingsFormGroup.controls.revealCharacterOnElimination.setValue(state.revealCharacterOnElimination);
			}
		};
		onRoomUpdate();
		dataService.roomUpdated.subscribe(() => onRoomUpdate());
	}

	onChangeSettings() {
		if (this.settingsFormGroup.value.characterSet != null && this.settingsFormGroup.value.enableTeams != null && this.settingsFormGroup.value.revealCharacterOnElimination != null) {
			this.sendUpdate({
				updateSettings: true,
				characterSet: this.settingsFormGroup.value.characterSet,
				startingCardsPerPlayer: this.settingsFormGroup.value.startingCardsPerPlayer ?? 2,
				startingCoinsPerPlayer: this.settingsFormGroup.value.startingCoinsPerPlayer ?? 2,
				enableTeams: this.settingsFormGroup.value.enableTeams,
				revealCharacterOnElimination: this.settingsFormGroup.value.revealCharacterOnElimination,
			});
		}
	}

	private sendUpdate(request: Request) {
		this.dataService.sendUpdate<Request>(request);
	}
}
