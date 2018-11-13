import React, {Component} from 'react';
import PropTypes from "prop-types";
import * as userService from "../services/UserService";
import {Link} from 'react-router-dom';
import ReactTable from 'react-table';
import 'react-table/react-table.css';
import {LabelInput, TextInput} from "../../common/ui/inputs";

export class UsersPage extends Component {
    columns = [
        {
            title: 'Identifiant utilisateur',
            notFilterable: true,
            content: item => item.userId,
            cell: (v, item) => {
                return <Link to={`/organisations/${item.orgKey}/users/${item.userId}`}
                             style={{cursor: 'pointer'}}>{item.userId}</Link>
            }
        },
        {
            title: 'Clé de l\'organisation',
            notFilterable: true,
            content: item => item.orgKey,
            cell: (v, item) => {
                return <Link to={`/organisations/${item.orgKey}`} style={{cursor: 'pointer'}}>{item.orgKey}</Link>
            }
        }
    ];

    state = {
        items: [],
        page: 0,
        pageSize: 20,
        count: 0,
        filterUserId: '',
        filterOrgKey: '',
        loading: true,
        tenant: undefined,
        organisationKey: undefined
    };

    componentWillMount() {
        this.setState({tenant: this.props.tenant, organisationKey: this.props.organisationKey});
    }

    fetchData = (state, instance) => {
        this.setState({loading: true});
        ((this.state.organisationKey) ?
                userService.getUsersByOrganisations(this.state.tenant, this.state.organisationKey, state.page, state.pageSize, this.state.filterUserId)
                :
                userService.getUsers(this.state.tenant, state.page, state.pageSize, this.state.filterUserId)
        ).then(pagedUsers => this.setState({
            items: pagedUsers.items,
            count: pagedUsers.count,
            page: pagedUsers.page,
            pageSize: pagedUsers.pageSize,
            loading: false
        }));
    };

    onChange = (search, name) => {
        this.setState({[name]: search});
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
                    <h3>Utilisateurs</h3>
                </div>
                <div className="col-md-12">
                    <div className="row">
                        <div className="col-md-10">
                            <TextInput label="Filtre identifiant utilisateur" value={this.state.filterUserId} onChange={(search) => this.onChange(search, "filterUserId")}/>
                        </div>
                        <div className="col-md-2">
                            <div className="btn btn-primary" onClick={() => this.fetchData({page: 0, pageSize: this.state.pageSize})}>Rechercher</div>
                        </div>
                    </div>
                </div>
                <div className="row">
                    <div className="col-md-12">
                        <ReactTable
                            className="fulltable -striped -highlight"
                            manual // Forces table not to paginate or sort automatically so we can handle it server-side
                            data={this.state.items}
                            filterable={false}
                            filterAll={true}
                            defaultSorted={[{id: this.columns[0].title, desc: false}]}
                            defaultFiltered={
                                [
                                ]}
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

UsersPage.propTypes = {
    tenant: PropTypes.string.isRequired,
    organisationKey: PropTypes.string
};
