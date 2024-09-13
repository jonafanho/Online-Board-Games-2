import {Routes} from "@angular/router";
import {LobbyComponent} from "./component/lobby/lobby.component";
import {HomeComponent} from "./component/home/home.component";
import {DebugComponent} from "./component/debug/debug.component";

export const routes: Routes = [
	{
		path: "game",
		children: [
			{
				path: "**",
				component: LobbyComponent,
			},
		],
	},
	{
		path: "debug",
		component: DebugComponent,
	},
	{
		path: "",
		component: HomeComponent,
	},
	{
		path: "**",
		redirectTo: "",
	},
];
