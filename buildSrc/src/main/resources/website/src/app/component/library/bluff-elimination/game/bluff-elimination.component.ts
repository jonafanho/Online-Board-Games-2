import {Component} from "@angular/core";
import {Action, Character, CharacterSet, ClientState, Event, Request, Stage} from "../../../../entity/generated/bluff_elimination";
import {DataService} from "../../../../service/data.service";
import {MatRadioModule} from "@angular/material/radio";
import {MatButtonModule} from "@angular/material/button";
import {MatCheckboxModule} from "@angular/material/checkbox";
import {ReactiveFormsModule} from "@angular/forms";
import {MatIconModule} from "@angular/material/icon";
import {MatCardModule} from "@angular/material/card";
import {PlayerCardComponent} from "../../../player-card/player-card.component";
import {Player} from "../../../../entity/player";
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {MatTooltipModule} from "@angular/material/tooltip";
import {MatBadgeModule} from "@angular/material/badge";
import {MatDividerModule} from "@angular/material/divider";
import {MatMenuModule} from "@angular/material/menu";
import {MatFormFieldModule} from "@angular/material/form-field";

@Component({
	selector: "game-bluff-elimination",
	standalone: true,
	imports: [
		MatRadioModule,
		MatButtonModule,
		MatCheckboxModule,
		ReactiveFormsModule,
		MatFormFieldModule,
		MatIconModule,
		MatCardModule,
		PlayerCardComponent,
		MatProgressSpinnerModule,
		MatTooltipModule,
		MatBadgeModule,
		MatDividerModule,
		MatMenuModule,
	],
	templateUrl: "./bluff-elimination.component.html",
	styleUrl: "./bluff-elimination.component.css",
})
export class BluffEliminationComponent {
	protected readonly playerDetails: { player: Player, characterNames: string [], coins: number, isWaiting: boolean, status: string }[] = [];
	protected readonly history: [string, string, string][][] = [];
	protected readonly actions: { title: string, disabled: boolean, targets?: { playerName: string, uuid: string }[], icon: string, characterName?: string, send: (target?: string) => void }[] = [];
	protected readonly checkBoxDetails: { characterName: string, click: (checked: boolean) => void }[] = [];
	protected readonly checkBoxButtonDetails: { title: string, disabled: () => boolean } = {title: "", disabled: () => true};
	protected isWaiting = false;
	protected isGameOver = false;
	private readonly selectedCharacters: Character[] = [];

	constructor(private readonly dataService: DataService) {
		const onRoomUpdate = () => {
			this.playerDetails.length = 0;
			this.history.length = 0;
			this.actions.length = 0;
			this.checkBoxDetails.length = 0;
			const state = this.getState();
			const thisPlayer = dataService.getPlayer();
			const playerMap: { [key: string]: { player: Player, isAlive: boolean, coins: number } } = {};
			dataService.getRoom()?.players.map(player => playerMap[player.uuid] = {player, isAlive: false, coins: 0});

			if (state && thisPlayer) {
				const waitingUuidList = state.waitingForPlayers.map(waitingForPlayer => waitingForPlayer.uuid);

				for (let i = 0; i < state.playerDetails.length; i++) {
					const playerDetailsEntry = state.playerDetails[i];
					const player = playerMap[playerDetailsEntry.uuid].player;

					if (player) {
						const cards: (Character | undefined)[] = [];
						playerDetailsEntry.visibleCharacters.forEach(card => cards.push(card));
						for (let j = 0; j < playerDetailsEntry.hiddenCharacters; j++) {
							cards.push(undefined);
						}
						BluffEliminationComponent.sortCards(cards);

						const isWaiting = waitingUuidList.includes(playerDetailsEntry.uuid);
						this.playerDetails.push({
							player,
							characterNames: cards.map(character => character ? BluffEliminationComponent.getCharacterName(character) : "?"),
							coins: playerDetailsEntry.coins,
							isWaiting,
							status: cards.length == 0 ? "Eliminated" : this.isGameOver ? "Winner!" : state.currentTurnPlayerIndex == i ? "Current Turn" : isWaiting ? "Waiting for Action" : "",
						});

						playerMap[playerDetailsEntry.uuid].isAlive = cards.length > 0;
						playerMap[playerDetailsEntry.uuid].coins = cards.length > 0 ? playerDetailsEntry.coins : 0;
					}
				}

				state.history.forEach(turnHistory => this.history.push(turnHistory.historyEvents.map(historyEvent => {
					const sender = playerMap[historyEvent.sender].player.formattedName;
					const target = historyEvent.target ? playerMap[historyEvent.target].player.formattedName : "";
					switch (historyEvent.event) {
						case Event.Income:
							return [`${sender} took income.`, "", ""];
						case Event.ForeignAid:
							return [`${sender} attempted to take foreign aid.`, "", ""];
						case Event.Eliminate:
							return [`${sender} eliminated ${target}.`, "", ""];
						case Event.Tax:
							return [`${sender} claimed`, "Duke", "and attempted to take tax."];
						case Event.Assassinate:
							return [`${sender} claimed`, "Assassin", `and attempted to assassinate ${target}.`];
						case Event.Steal:
							return [`${sender} claimed`, "Captain", `and attempted to steal from ${target}.`];
						case Event.ExchangeAmbassador:
							return [`${sender} claimed`, "Ambassador", "and attempted to exchange cards from the deck."];
						case Event.ExchangeInquisitor:
							return [`${sender} claimed`, "Inquisitor", "and attempted to exchange cards from the deck."];
						case Event.Peek:
							return [`${sender} claimed`, "Inquisitor", `and attempted to look at one of ${target}'s cards.`];
						case Event.BlockForeignAid:
							return [`${sender} claimed`, "Duke", "and attempted to block the foreign aid."];
						case Event.BlockAssassination:
							return [`${sender} claimed`, "Contessa", "and attempted to block the assassination."];
						case Event.BlockStealingCaptain:
							return [`${sender} claimed`, "Captain", "and attempted to block the stealing."];
						case Event.BlockStealingAmbassador:
							return [`${sender} claimed`, "Ambassador", "and attempted to block the stealing."];
						case Event.BlockStealingInquisitor:
							return [`${sender} claimed`, "Inquisitor", "and attempted to block the stealing."];
						case Event.Challenge:
							return [`${sender} challenged ${target}.`, "", ""];
						case Event.Elimination:
							if (historyEvent.visibleCharacters.length == 0) {
								return [`${sender} eliminated ${historyEvent.hiddenCharacters} character${historyEvent.hiddenCharacters > 1 ? "s" : ""}.`, "", ""];
							} else {
								return [`${sender} eliminated ${historyEvent.visibleCharacters.map(BluffEliminationComponent.getCharacterName).join(" and ")}.`, "", ""];
							}
					}
				})));

				this.isGameOver = state.stage == Stage.End;
				this.isWaiting = waitingUuidList.includes(thisPlayer.uuid);
				const thisPlayerDetails = state.playerDetails.find(playerDetailsEntry => playerDetailsEntry.uuid == thisPlayer.uuid);

				if (this.isWaiting && thisPlayerDetails) {
					const historyEvents = state.history[state.history.length - 1].historyEvents;
					const getIcon = (character: Character) => thisPlayerDetails.visibleCharacters.includes(character) ? "check_mark" : "warning";
					const getTargets = (condition: (playerDetails: { player: Player, isAlive: boolean, coins: number }) => boolean) => Object.values(playerMap).filter(playerDetails => condition(playerDetails) && thisPlayer.uuid != playerDetails.player.uuid).map(playerDetails => ({playerName: playerDetails.player.formattedName, uuid: playerDetails.player.uuid}));
					BluffEliminationComponent.sortCards(thisPlayerDetails.visibleCharacters);
					const writeCheckBoxes = (title: string) => {
						thisPlayerDetails.visibleCharacters.forEach(character => this.checkBoxDetails.push({
							characterName: BluffEliminationComponent.getCharacterName(character),
							click: checked => {
								if (checked) {
									this.selectedCharacters.push(character);
								} else {
									this.selectedCharacters.splice(this.selectedCharacters.indexOf(character), 1);
								}
							},
						}));
						this.checkBoxButtonDetails.title = title;
						this.checkBoxButtonDetails.disabled = () => state.waitingForPlayers.find(waitingForPlayer => waitingForPlayer.eliminate)?.eliminate !== this.selectedCharacters.length;
					};

					if (historyEvents.length == 0) {
						this.actions.push({
							title: "Income",
							disabled: false,
							icon: "",
							send: () => this.sendAction(Action.Income),
						});
						this.actions.push({
							title: "Foreign Aid",
							disabled: false,
							icon: "",
							send: () => this.sendAction(Action.ForeignAid),
						});
						this.actions.push({
							title: "Eliminate",
							disabled: thisPlayerDetails.coins < 7,
							targets: getTargets(playerDetails => playerDetails.isAlive),
							icon: "",
							send: target => this.sendAction(Action.Eliminate, target),
						});
						this.actions.push({
							title: "Tax",
							disabled: false,
							icon: getIcon(Character.Duke),
							characterName: BluffEliminationComponent.getCharacterName(Character.Duke),
							send: () => this.sendAction(Action.Tax),
						});
						this.actions.push({
							title: "Assassinate",
							disabled: thisPlayerDetails.coins < 3,
							targets: getTargets(playerDetails => playerDetails.isAlive),
							icon: getIcon(Character.Assassin),
							characterName: BluffEliminationComponent.getCharacterName(Character.Assassin),
							send: target => this.sendAction(Action.Assassinate, target),
						});
						this.actions.push({
							title: "Steal",
							disabled: false,
							targets: getTargets(playerDetails => playerDetails.coins >= 2),
							icon: getIcon(Character.Captain),
							characterName: BluffEliminationComponent.getCharacterName(Character.Captain),
							send: target => this.sendAction(Action.Steal, target),
						});
						if (state.characterSet == CharacterSet.WithAmbassador) {
							this.actions.push({
								title: "Exchange",
								disabled: false,
								icon: getIcon(Character.Ambassador),
								characterName: BluffEliminationComponent.getCharacterName(Character.Ambassador),
								send: () => this.sendAction(Action.ExchangeAmbassador),
							});
						} else {
							this.actions.push({
								title: "Exchange",
								disabled: false,
								icon: getIcon(Character.Inquisitor),
								characterName: BluffEliminationComponent.getCharacterName(Character.Inquisitor),
								send: () => this.sendAction(Action.ExchangeInquisitor),
							});
							this.actions.push({
								title: "Peek",
								disabled: false,
								icon: getIcon(Character.Inquisitor),
								characterName: BluffEliminationComponent.getCharacterName(Character.Inquisitor),
								send: () => this.sendAction(Action.Peek),
							});
						}
					} else {
						const addAcceptButton = () => this.actions.push({
							title: "Accept",
							disabled: false,
							icon: "",
							send: () => this.sendAccept(),
						});
						const addChallengeAcceptButtons = () => {
							this.actions.push({
								title: "Challenge",
								disabled: false,
								icon: "",
								send: () => this.sendChallenge(),
							});
							addAcceptButton();
						};

						for (let i = historyEvents.length - 1; i >= 0; i--) {
							const lastEvent = historyEvents[i];
							switch (lastEvent.event) {
								case Event.ForeignAid:
									this.actions.push({
										title: "Block Foreign Aid",
										disabled: false,
										icon: getIcon(Character.Duke),
										characterName: BluffEliminationComponent.getCharacterName(Character.Duke),
										send: () => this.sendAction(Action.BlockForeignAid),
									});
									addAcceptButton();
									break;
								case Event.ExchangeAmbassador:
									if (lastEvent.sender == thisPlayer.uuid) {
										writeCheckBoxes("Discard Selected");
									} else {
										addChallengeAcceptButtons();
									}
									break;
								case Event.ExchangeInquisitor:
									if (lastEvent.sender == thisPlayer.uuid) {
										writeCheckBoxes("Discard Selected");
									} else {
										addChallengeAcceptButtons();
									}
									break;
								case Event.Tax:
								case Event.Peek:
								case Event.BlockForeignAid:
								case Event.BlockAssassination:
								case Event.BlockStealingCaptain:
								case Event.BlockStealingAmbassador:
								case Event.BlockStealingInquisitor:
									addChallengeAcceptButtons();
									break;
								case Event.Assassinate:
									if (lastEvent.target == thisPlayer.uuid) {
										this.actions.push({
											title: "Block Assassination",
											disabled: false,
											icon: getIcon(Character.Contessa),
											characterName: BluffEliminationComponent.getCharacterName(Character.Contessa),
											send: () => this.sendAction(Action.BlockAssassination),
										});
									}
									addChallengeAcceptButtons();
									break;
								case Event.Steal:
									if (lastEvent.target == thisPlayer.uuid) {
										this.actions.push({
											title: "Block Steal with Captain",
											disabled: false,
											icon: getIcon(Character.Captain),
											characterName: BluffEliminationComponent.getCharacterName(Character.Captain),
											send: () => this.sendAction(Action.BlockStealingCaptain),
										});
										if (state.characterSet == CharacterSet.WithAmbassador) {
											this.actions.push({
												title: "Block Steal with Ambassador",
												disabled: false,
												icon: getIcon(Character.Ambassador),
												characterName: BluffEliminationComponent.getCharacterName(Character.Ambassador),
												send: () => this.sendAction(Action.BlockStealingAmbassador),
											});
										} else {
											this.actions.push({
												title: "Block Steal with Inquisitor",
												disabled: false,
												icon: getIcon(Character.Inquisitor),
												characterName: BluffEliminationComponent.getCharacterName(Character.Inquisitor),
												send: () => this.sendAction(Action.BlockStealingInquisitor),
											});
										}
									}
									addChallengeAcceptButtons();
									break;
								case Event.Challenge:
									writeCheckBoxes("Eliminate Selected");
									break;
							}

							if (this.actions.length > 0 || this.checkBoxDetails.length > 0) {
								break;
							}
						}
					}
				} else {
					this.selectedCharacters.length = 0;
					this.actions.push({
						title: "Tell others to hurry up",
						disabled: false,
						icon: "schedule",
						send: () => console.log("no"),
					});
				}
			}
		};

		dataService.roomUpdated.subscribe(() => onRoomUpdate());
		onRoomUpdate();
	}

	protected sendSelectCharacters() {
		this.sendUpdate({selectCharacters: this.selectedCharacters});
	}

	private sendAction(action: Action, target?: string) {
		this.sendUpdate({playAction: {action, target}});
	}

	private sendChallenge() {
		this.sendUpdate({challenge: true});
	}

	private sendAccept() {
		this.sendUpdate({accept: true});
	}

	private sendUpdate(request: Request) {
		this.dataService.sendUpdate<Request>(request);
	}

	private getState() {
		return this.dataService.getRoom()?.getState<ClientState, Stage>();
	}

	private static sortCards(cards: (Character | undefined)[]) {
		const charactersArray = Object.values(Character);
		cards.sort((character1, character2) => (character1 ? charactersArray.indexOf(character1) : charactersArray.length) - (character2 ? charactersArray.indexOf(character2) : charactersArray.length));
	}

	private static getCharacterName(character: Character) {
		switch (character) {
			case Character.Duke:
				return "Duke";
			case Character.Assassin:
				return "Assassin";
			case Character.Captain:
				return "Captain";
			case Character.Ambassador:
				return "Ambassador";
			case Character.Inquisitor:
				return "Inquisitor";
			case Character.Contessa:
				return "Contessa";
		}
	}
}
