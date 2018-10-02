import React, {Component} from 'react';

import ReactTable from 'react-table';
import 'react-table/react-table.css';
import Moment from 'react-moment';
import {UploadFilePage} from "./UploadFilePage";


export class UserExtractPage extends Component {
    websocket = null;
    ping = null;

    columns = [
        {
            title: 'Tenant',
            content: item => item.payload.tenant,
            notFilterable: true
        },
        {
            title: 'Organisation',
            content: item => item.payload.orgKey,
            notFilterable: true
        },
        {
            title: 'Utilisateur',
            content: item => item.payload.userId,
            notFilterable: true
        },
        {
            title: 'Type de la demande',
            content: item => item.type,
            notFilterable: true
        }, {
            title: 'Demandé le',
            notFilterable: true,
            content: item => item.payload.startedAt,
            cell: (v, item) => {
                return item.payload.startedAt ?
                    <Moment locale="fr" parse="YYYY-MM-DDTHH:mm:ssZ"
                            format="DD/MM/YYYY HH:mm:ss">{item.payload.startedAt}</Moment> : "NC";
            }
        }, {
            title: 'Traité le',
            notFilterable: true,
            content: item => item.payload.startedAt,
            cell: (v, item) => {
                return item.payload.endedAt ?
                    <Moment locale="fr" parse="YYYY-MM-DDTHH:mm:ssZ"
                            format="DD/MM/YYYY HH:mm:ss">{item.payload.endedAt}</Moment> : "NC";
            }
        },
        {
            title: `Action`,
            content: item => item.payload.userId,
            cell: (v, item) => {
                return item.type === "UserExtractTaskAsked" ?
                    <a onClick={() => this.setState({selectedEvent: item})}
                       style={{cursor: 'pointer'}}><i className="glyphicon glyphicon-share"/></a>
                    :
                    ""
            },
            notFilterable: true
        }
    ];

    state = {
        messages: [],
        selectedEvent: null,
        url: null
    };

    componentDidMount() {
        this.connectWebSocket();
    }

    componentWillUnmount() {
        this.closeWebSocket();
    }

    connectWebSocket = () => {
        if (!this.websocket) {
            console.log("create a websocket");
            this.websocket = new WebSocket(`${this.props.webSocketHost}/ws`);

            this.websocket.onopen = () => {
                console.log("ws is open")
            };

            this.websocket.onmessage = (msg) => {
                console.log(`message received ${msg.data}`);
                const messages = [...this.state.messages];
                messages.push(JSON.parse(msg.data));
                this.setState({messages: this.cleanMessages(messages)});
            };

            if (!this.ping)
                this.ping = setInterval(() => {
                    if (this.websocket.readyState === WebSocket.OPEN) {
                        this.websocket.send("iAmAlive");
                    }
                }, 500);
        }
    };

    cleanMessages = (allMessages) => {
        const messages = [];

        for (let i = allMessages.length - 1; i >= 0; i--) {
            let msg = allMessages[i];
            if (messages.findIndex(message => message.payload._id === msg.payload._id) === -1) {
                messages.push(msg)
            }
        }

        return messages;
    };

    closeWebSocket = () => {
        if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
            this.websocket.close();

            this.websocket = null;

            if (this.ping) {
                clearInterval(this.ping);
                this.ping = null;
            }
        }
    };

    onUpload = (url) => {
        this.setState({selectedEvent: null, url})
    };

    render() {

        const columns = this.columns.map(c => {
            return {
                Header: c.title,
                id: c.title,
                headerStyle: c.style,
                width: c.style && c.style.width ? c.style.width : undefined,
                style: {...c.style, height: 30},
                sortable: !c.notSortable,
                filterable: !c.notFilterable,
                accessor: d => (c.content ? c.content(d) : d),
                Filter: d => (
                    <input
                        type="text"
                        className="form-control input-sm"
                        value={c.filteredState ? this.state[c.filteredState] : ''}
                        onChange={e => {
                            this.onChange(e.target.value, c.filteredState)
                        }}
                        placeholder="Search ..."
                    />
                ),
                Cell: r => {
                    const value = r.value;
                    const original = r.original;
                    return c.cell ? (
                        c.cell(value, original, this)
                    ) : (
                        value
                    );
                },
            };
        });

        return (
            <div className="row">
                <div className="col-md-12">
                    <h3>Demande de téléchargement de données</h3>
                </div>

                <div className="row">
                    <div className="col-md-12">
                        <ReactTable
                            className="fulltable -striped -highlight"
                            manual // Forces table not to paginate or sort automatically so we can handle it server-side
                            data={this.state.messages}
                            sortable={false}
                            filterable={false}
                            filterAll={true}
                            defaultFiltered={
                                []
                            }
                            defaultPageSize={5}
                            columns={columns}
                            defaultFilterMethod={(filter, row, column) => {
                                const id = filter.pivotId || filter.id;
                                if (row[id] !== undefined) {
                                    const value = String(row[id]);
                                    return value.toLowerCase().indexOf(filter.value.toLowerCase()) > -1;
                                } else {
                                    return true;
                                }
                            }}
                        />
                    </div>
                </div>

                {
                    this.state.selectedEvent &&
                    <div className="col-md-12">
                        <UploadFilePage tenant={this.state.selectedEvent.payload.tenant}
                                        organisationKey={this.state.selectedEvent.payload.orgKey}
                                        userId={this.state.selectedEvent.payload.userId} onUpload={this.onUpload}/>
                    </div>
                }

                {
                    this.state.url &&
                    <div className="col-md-12">
                        <a href={this.state.url} target="_blank">Voir le fichier téléchargé</a>
                    </div>
                }
            </div>
        )
    }
}