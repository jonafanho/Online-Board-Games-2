import UTILITIES from "./utilities.js";

const setup = section => Array.from(document.getElementsByClassName("section")).forEach(element => UTILITIES.setAttribute(element, "hidden", element.id !== section));
const SOCKET = io("localhost:8888");
SOCKET.on("connect", () => UTILITIES.send(SOCKET, "init", {}));
document.getElementById("text-box-name").value = UTILITIES.getCookie("name");

UTILITIES.receive(SOCKET, "home", () => {
	const buttonCreateRoom = document.getElementById("button-create-room");
	const textBoxRoomCode = document.getElementById("text-box-room-code");
	const buttonJoinRoom = document.getElementById("button-join-room");

	const disableControls = (disabled1, disabled2) => {
		UTILITIES.setAttribute(buttonCreateRoom, "disabled", disabled1);
		UTILITIES.setAttribute(textBoxRoomCode, "disabled", disabled1);
		UTILITIES.setAttribute(buttonJoinRoom, "disabled", disabled2 || textBoxRoomCode.value.length !== 4);
	};

	textBoxRoomCode.oninput = () => {
		textBoxRoomCode.value = textBoxRoomCode.value.toUpperCase().replaceAll(/[^A-Z0-9]/g, "").substring(0, 4);
		disableControls(false, false);
	};

	buttonCreateRoom.onclick = () => {
		disableControls(true, true);
		UTILITIES.send(SOCKET, "create-room", {});
	};

	buttonJoinRoom.onclick = () => {
		disableControls(true, true);
		UTILITIES.send(SOCKET, "join-room", {code: textBoxRoomCode.value});
	};

	disableControls(false, false);
	setup("section-home");
});

UTILITIES.receive(SOCKET, "lobby", data => {
	const {code, players, host} = data;

	document.getElementById("text-room-code").innerText = code;
	document.getElementById("text-players").innerText = `Players (${players.length})`;
	UTILITIES.setAttribute(document.getElementById("button-start-game"), "disabled", players.length < 5 || players.length > 10);
	const buttonLeaveGame = document.getElementById("button-leave-game");
	buttonLeaveGame.setAttribute("value", host ? "Delete Room" : "Leave Game");
	const textBoxName = document.getElementById("text-box-name");
	textBoxName.value = UTILITIES.getCookie("name");
	const buttonChangeName = document.getElementById("button-change-name");
	const groupPlayers = document.getElementById("group-players");
	groupPlayers.innerText = "";
	players.forEach(player => groupPlayers.innerText += player);

	const disableControls = () => {
		const name = textBoxName.value;
		UTILITIES.setAttribute(buttonChangeName, "disabled", name === "" || name === UTILITIES.getCookie("name"));
	};

	buttonLeaveGame.onclick = () => UTILITIES.send(SOCKET, "leave-room", {code: code});

	textBoxName.oninput = () => disableControls();

	buttonChangeName.onclick = () => {
		UTILITIES.setCookie("name", textBoxName.value);
		disableControls();
		UTILITIES.send(SOCKET, "join-room", {code: code});
	};

	disableControls();
	setup("section-lobby");
});

UTILITIES.receive(SOCKET, "error-no-room", () => document.getElementById("text-error").innerText = "Room does not exist!");
