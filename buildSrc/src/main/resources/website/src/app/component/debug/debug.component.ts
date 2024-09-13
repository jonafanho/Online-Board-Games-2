import {Component} from "@angular/core";
import {MatInputModule} from "@angular/material/input";
import {MatButtonModule} from "@angular/material/button";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatIcon} from "@angular/material/icon";
import {MatTooltip} from "@angular/material/tooltip";
import {DomSanitizer, SafeResourceUrl} from "@angular/platform-browser";
import {JoinRoomFormComponent} from "../join-room-form/join-room-form.component";

@Component({
	selector: "app-debug",
	standalone: true,
	imports: [
		MatInputModule,
		MatButtonModule,
		FormsModule,
		MatIcon,
		MatTooltip,
		ReactiveFormsModule,
		JoinRoomFormComponent,
	],
	templateUrl: "./debug.component.html",
	styleUrl: "./debug.component.css",
})
export class DebugComponent {
	protected readonly instances: { url: SafeResourceUrl, width: string, height: string }[] = [];

	constructor(private sanitizer: DomSanitizer) {
		this.createInstances(4);
	}

	protected onChangeInstances(numericInput: HTMLInputElement) {
		try {
			this.createInstances(parseInt(numericInput.value));
		} catch (e) {
		}
	}

	protected onJoinRoomForAll(roomCode: string) {
		console.log(`${document.location.origin}/game/${roomCode}?debug=true`);
		for (let i = 0; i < this.instances.length; i++) {
			this.instances[i].url = this.sanitizer.bypassSecurityTrustResourceUrl(`${document.location.origin}/game/${roomCode}${i > 0 ? `?debugIndex=${i}` : ""}`);
		}
	}

	protected onRefresh() {
		for (let i = 0; i < this.instances.length; i++) {
			(document.getElementById(`instance_${i}`) as HTMLIFrameElement).contentDocument?.location.reload();
		}
	}

	private createInstances(count: number) {
		this.instances.length = 0;
		const rows = Math.max(1, Math.floor(Math.sqrt(count)));
		const columns = Math.ceil(count / rows);
		for (let i = 0; i < count; i++) {
			this.instances.push({url: this.sanitizer.bypassSecurityTrustResourceUrl(`${document.location.origin}${i > 0 ? `?debugIndex=${i}` : ""}`), width: `${100 / columns}%`, height: `${100 / rows}%`});
		}
	}
}
