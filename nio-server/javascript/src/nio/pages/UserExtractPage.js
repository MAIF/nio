import React, {Component} from 'react';
import PropTypes from "prop-types";
import * as userExtractService from "../services/UserExtractService";
import Moment from 'react-moment';

import {Link} from 'react-router-dom';
import ReactTable from 'react-table';
import 'react-table/react-table.css';


export class UserExtractPage extends Component {

    columns = [
        {
            title: 'Utilisateur',
            content: item => item.key,
            cell: (v, item) => {
                return <Link to={`/organisations/${this.props.organisationKey}/users/${item.userId}`}
                             style={{cursor: 'pointer'}}>{item.userId} <i
                    className="glyphicon glyphicon-share"/></Link>
            },
            notFilterable: true
        }, {
            title: 'Date de début',
            notFilterable: true,
            content: item => item.startedAt,
            cell: (v, item) => {
                return item.startedAt ?
                    <Moment locale="fr" parse="YYYY-MM-DDTHH:mm:ssZ"
                            format="DD/MM/YYYY HH:mm:ss">{item.startedAt}</Moment> : "NC"
            }
        }, {
            title: 'Date de début de téléchargement',
            notFilterable: true,
            content: item => item.uploadStartedAt,
            cell: (v, item) => {
                return item.uploadStartedAt ?
                    <Moment locale="fr" parse="YYYY-MM-DDTHH:mm:ssZ"
                            format="DD/MM/YYYY HH:mm:ss">{item.uploadStartedAt}</Moment> : "NC"
            }
        }, {
            title: 'Date de fin',
            notFilterable: true,
            content: item => item.endedAt,
            cell: (v, item) => {
                return item.endedAt ?
                    <Moment locale="fr" parse="YYYY-MM-DDTHH:mm:ssZ"
                            format="DD/MM/YYYY HH:mm:ss">{item.endedAt}</Moment> : "NC"
            }
        }
    ];

    state = {
        items: [],
        page: 0,
        pageSize: 20,
        count: 0,
        loading: true
    };


    onChange = (search, name) => {
        this.setState({[name]: search});
    };

    fetchData = (state, instance) => {
        this.fetchExtractedUser(this.props.tenant, this.props.organisationKey, this.props.userId, state.page, state.pageSize);
    };

    fetchExtractedUser = (tenant, organisationKey, userId, page, pageSize) => {
        this.setState({loading: true}, () => {
            if (userId) {
                // fetch extract history for an organisation
                userExtractService.fetchUserExtractHistory(tenant, organisationKey, userId, page, pageSize)
                    .then(pagedUserExtraction => this.setState({
                        items: pagedUserExtraction.items,
                        count: pagedUserExtraction.count,
                        page: pagedUserExtraction.page,
                        pageSize: pagedUserExtraction.pageSize,
                        loading: false
                    }));
            } else {
                // fetch extract history for an organisation
                userExtractService.fetchExtractHistory(tenant, organisationKey, page, pageSize)
                    .then(pagedUserExtraction => this.setState({
                        items: pagedUserExtraction.items,
                        count: pagedUserExtraction.count,
                        page: pagedUserExtraction.page,
                        pageSize: pagedUserExtraction.pageSize,
                        loading: false
                    }))
            }
        });
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
                    <h1>Extractions</h1>
                </div>

                <div className="row">
                    <div className="col-md-12">
                        <ReactTable
                            className="fulltable -striped -highlight"
                            manual // Forces table not to paginate or sort automatically so we can handle it server-side
                            data={this.state.items}
                            sortable={false}
                            filterable={false}
                            filterAll={true}
                            defaultFiltered={
                                []
                            }
                            pages={Math.ceil(this.state.count / this.state.pageSize)} //Display to toal number of pages
                            loading={this.state.loading}
                            onFetchData={(state, instance) => {
                                this.fetchData(state, instance)
                            }}
                            defaultPageSize={20}
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
            </div>
        );
    }
}

UserExtractPage.propTypes = {
    tenant: PropTypes.string.isRequired,
    organisationKey: PropTypes.string.isRequired,
    userId: PropTypes.string
};