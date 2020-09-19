import { Component, OnInit, ViewChild } from '@angular/core';
import { MatSort } from '@angular/material/sort';

@Component({
  selector: 'app-offers-history',
  templateUrl: './offers-history.component.html',
  styleUrls: ['./offers-history.component.scss']
})
export class OffersHistoryComponent implements OnInit {

  constructor() { }

  @ViewChild(MatSort) sort: MatSort;
  
  displayedColumns: string[] = ['offerID','offerTitle', 'date'];
  transactions: Transaction[] = [
    {item: 'Beach ball', date: 4},
    {item: 'Towel', date: 5},
    {item: 'Frisbee', date: 2},
    {item: 'Sunscreen', date: 4},
    {item: 'Cooler', date: 25},
    {item: 'Swim suit', date: 15},
  ];

  sortData(sort: MatSort) {
    const data = this.transactions.slice();
    if (!sort.active || sort.direction === '') {
      this.transactions = data;
      return;
    }

    this.transactions = data.sort((a, b) => {
      const isAsc = sort.direction === 'asc';
      switch (sort.active) {
        case 'date': return this.compare(a.date, b.date, isAsc);
        default: return 0;
      }
    });
  }
  
  compare(a: number | string, b: number | string, isAsc: boolean) { return (a < b ? -1 : 1) * (isAsc ? 1 : -1);  }


  ngOnInit(): void { }

 
}
interface Transaction{
  item:String,
  date:number
}


export interface HistoryDTO{
  date:Date,
  action:String,
  offerId:number
}
