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
		const parsed = Number.parseInt(UTILITIES.getCookie("id"), 16);
		const id = (isNaN(parsed) ? Math.round(Math.random() * (2E14)) : parsed).toString(16);
		UTILITIES.setCookie("id", id);
		content["id"] = id;
		content["name"] = UTILITIES.getCookie("name");
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
