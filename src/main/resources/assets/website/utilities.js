const UTILITIES = {
	getCookie: name => {
		const splitCookies = document.cookie.split("; ").filter(cookie => cookie.startsWith(name + "="));
		if (splitCookies.length > 0 && splitCookies[0].includes("=")) {
			return decodeURIComponent(splitCookies[0].split("=")[1]);
		} else {
			return "";
		}
	},
	setCookie: (name, value) => document.cookie = `${name}=${value}; expires=${new Date(2999, 11, 31).toUTCString()}; path=/`,
	setAttribute: (element, attribute, set) => {
		if (set) {
			element.setAttribute(attribute, "");
		} else {
			element.removeAttribute(attribute);
		}
	},
	send: (socket, channel, content) => {
		const parsedId = Number.parseInt(UTILITIES.getCookie("id"), 16);
		const id = (isNaN(parsedId) ? Math.round(Math.random() * (2E14)) : parsedId).toString(16);
		UTILITIES.setCookie("id", id);
		content["id"] = id;

		const rawName = UTILITIES.getCookie("name");
		const name = rawName === "" ? "Player" : rawName;
		UTILITIES.setCookie("name", name);
		content["name"] = name;

		socket.emit(channel, JSON.stringify(content));
	},
	receive: (socket, channel, callback) => socket.on(channel, data => {
		if (data === undefined) {
			callback();
		} else {
			callback(JSON.parse(data));
		}
	}),
};

export default UTILITIES;
