import {Component} from "@angular/core";
import {CharacterSet, ClientState, Request, Stage} from "../../../../entity/generated/bluff_elimination";
import {DataService} from "../../../../service/data.service";
import {MatRadioModule} from "@angular/material/radio";
import {MatButtonModule} from "@angular/material/button";
import {MatCheckboxModule} from "@angular/material/checkbox";
import {FormControl, FormGroup, ReactiveFormsModule} from "@angular/forms";

@Component({
	selector: "game-settings-bluff-elimination",
	standalone: true,
	imports: [
		MatRadioModule,
		MatButtonModule,
		MatCheckboxModule,
		ReactiveFormsModule,
	],
	templateUrl: "./bluff-elimination-settings.component.html",
	styleUrl: "./bluff-elimination-settings.component.css",
})
export class BluffEliminationSettingsComponent {
	protected readonly CharacterSet = CharacterSet;
	protected readonly settingsFormGroup = new FormGroup({
		characterSet: new FormControl(CharacterSet.WithAmbassador),
		revealCharacterOnElimination: new FormControl(false),
	});

	constructor(private readonly dataService: DataService) {
		const updateControls = () => {
			const state = this.dataService.getRoom()?.getState<ClientState, Stage>();
			if (state) {
				this.settingsFormGroup.controls.characterSet.setValue(state.characterSet);
				this.settingsFormGroup.controls.revealCharacterOnElimination.setValue(state.revealCharacterOnElimination);
			}
		};
		updateControls();
		dataService.roomUpdated.subscribe(() => updateControls());
	}

	onChangeSettings() {
		if (this.settingsFormGroup.value.characterSet != null && this.settingsFormGroup.value.revealCharacterOnElimination != null) {
			this.sendUpdate({
				updateSettings: true,
				characterSet: this.settingsFormGroup.value.characterSet,
				revealCharacterOnElimination: this.settingsFormGroup.value.revealCharacterOnElimination,
			});
		}
	}

	private sendUpdate(request: Request) {
		this.dataService.sendUpdate<Request>(request);
	}
}
