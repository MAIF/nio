import React, {Component} from 'react';
import * as accountService from "../services/AccountService";
import {Link} from 'react-router-dom';
import ReactTable from 'react-table';
import 'react-table/react-table.css';
import {BooleanInput} from "../../common/ui/inputs";

export class AccountsPage extends Component {
    columns = [
        {
            title: 'Email',
            notFilterable: true,
            content: item => item.email,
            cell: (v, item) => {
                return <Link to={`/accounts/${item._id}`}
                             style={{cursor: 'pointer'}}>{item.email}</Link>
            }
        },
        {
            title: 'Administrateur',
            class:'center-content',
            notFilterable: true,
            content: item => item.isAdmin,
            cell: (v, item) => {
                return <BooleanInput value={item.isAdmin}
                                     onChange={(v) => this.updateItem(item, v)}
                                     label={""}
                />
            }
        },
        {
            title: 'Actions',
            notFilterable: true,
            content: item => item._id,
            cell: (v, item) => {
                return (
                    <div className="form-buttons text-center">
                        <Link to={`/accounts/${item._id}`}
                              style={{cursor: 'pointer'}}>
                            <button className="btn btn-success btn-xs">
                                <span className="glyphicon glyphicon-pencil" />
                            </button>
                        </Link>

                        <button className="btn btn-danger btn-xs" onClick={() => this.deleteItem(item._id)}>
                            <span className="glyphicon glyphicon-trash" />
                        </button>
                    </div>
                )
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

    deleteItem = (id) => {
        accountService.deleteAccount(id)
            .then(() => this.fetchData(this.state))
    };

    updateItem = (item, value) => {
        const account = {...item};
        account.isAdmin = value;

        accountService.updateAccount(account._id, account)
            .then(() => this.fetchData(this.state))
    };

    fetchData = (state, instance) => {
        this.setState({loading: true});

        accountService.getAccounts(state.page, state.pageSize)
            .then(pagedAccounts => this.setState({
                items: pagedAccounts.items,
                count: pagedAccounts.count,
                page: pagedAccounts.page,
                pageSize: pagedAccounts.pageSize,
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
                className : c.class,
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
                    <h3>Comptes</h3>
                </div>
                <div className="col-md-12 clearfix" style={{marginBottom: 20}}>
                    <Link className="btn btn-primary pull-right" to="/accounts/new" style={{cursor: 'pointer'}}>Nouveau compte</Link>
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

AccountsPage.propTypes = {};
