import {Routes} from "@angular/router";
import {LobbyComponent} from "./component/lobby/lobby.component";
import {HomeComponent} from "./component/home/home.component";

export const routes: Routes = [
	{
		path: "lobby",
		children: [
			{
				path: "**",
				component: LobbyComponent,
			},
		],
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
