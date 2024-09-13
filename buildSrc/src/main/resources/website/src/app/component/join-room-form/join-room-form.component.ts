import {Component, EventEmitter, Output} from "@angular/core";
import {MatIconModule} from "@angular/material/icon";
import {MatCardModule} from "@angular/material/card";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatFormField, MatLabel} from "@angular/material/form-field";
import {MatIconButton} from "@angular/material/button";
import {MatInput} from "@angular/material/input";
import {MatTooltip} from "@angular/material/tooltip";

@Component({
	selector: "app-join-room-form",
	standalone: true,
	imports: [
		MatIconModule,
		MatCardModule,
		FormsModule,
		MatFormField,
		MatIconButton,
		MatInput,
		MatLabel,
		MatTooltip,
		ReactiveFormsModule,
	],
	templateUrl: "./join-room-form.component.html",
	styleUrl: "./join-room-form.component.css",
})
export class JoinRoomFormComponent {
	@Output() joinRoom = new EventEmitter<string>();
	protected roomCodeValue = "";
	protected joinRoomDisabled = true;

	protected textChanged(newValue: string) {
		this.roomCodeValue = newValue.toUpperCase().replaceAll(/[^A-Z0-9]/g, "");
		this.joinRoomDisabled = this.roomCodeValue.length < 6;
	}

	protected onJoinRoom(roomCode: string) {
		this.joinRoom.emit(roomCode);
	}
}
