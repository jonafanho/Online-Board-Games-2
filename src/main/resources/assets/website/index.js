import UTILITIES from "./utilities.js";

const SOCKET = io("localhost:8888");

SOCKET.on("connect", () => UTILITIES.send(SOCKET, "init", {}));
UTILITIES.receive(SOCKET, "home", () => setup("section-home"));
UTILITIES.receive(SOCKET, "lobby", () => setup("section-lobby"));
UTILITIES.receive(SOCKET, "error-no-room", () => document.getElementById("text-error").innerText = "Room does not exist!");

const setup = section => {
	Array.from(document.getElementsByClassName("section")).forEach(element => UTILITIES.setAttribute(element, "hidden", element.id !== section));

	const buttonCreateRoom = document.getElementById("button-create-room");
	const textBoxRoomCode = document.getElementById("text-box-room-code");
	const buttonJoinRoom = document.getElementById("button-join-room");
	const disableHomeControls = (disabled1, disabled2) => {
		UTILITIES.setAttribute(buttonCreateRoom, "disabled", disabled1);
		UTILITIES.setAttribute(textBoxRoomCode, "disabled", disabled1);
		UTILITIES.setAttribute(buttonJoinRoom, "disabled", disabled2 || textBoxRoomCode.value.length !== 4);
	};
	disableHomeControls(false, false);

	textBoxRoomCode.oninput = () => {
		textBoxRoomCode.value = textBoxRoomCode.value.toUpperCase().replaceAll(/[^A-Z0-9]/g, "").substring(0, 4);
		disableHomeControls(false, false);
	};

	buttonCreateRoom.onclick = () => {
		disableHomeControls(true, true);
		UTILITIES.send(SOCKET, "create-room", {});
	};

	buttonJoinRoom.onclick = () => {
		disableHomeControls(true, true);
		UTILITIES.send(SOCKET, "join-room", {code: textBoxRoomCode.value});
	};
};
