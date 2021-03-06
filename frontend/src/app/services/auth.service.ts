import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Config } from './app-config';
import { UserDTO } from 'src/models/interfaces';
import { CookieService } from 'ngx-cookie-service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { interval, Observable } from 'rxjs';
import { delay,retry, retryWhen } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {


  public isAuthenticated = false;
  public isOnline;

  constructor(private config:Config ,private http:HttpClient,private cookieService:CookieService,private router: Router) {  }

  public redirectIfAuthenticated(){
      if(this.isAuthenticated)
      this.router.navigate(['/search']);
  }

  public auth(login:String,password:String){
    return this.http.post<string>(this.config.API_URL_SERVER.concat(this.config.LOGIN_USER_ENDPOINT),{username:login,password:password},this.config.HEADER)
  }

  public register(userDTO:UserDTO){
      return this.http.post<String>(this.config.API_URL_SERVER.concat(this.config.REGISTER_USER_ENDPOINT),userDTO,this.config.HEADER)
  }

  public getAuthHeader() { return { headers:{"Content-type":"application/json","Authorization":"Bearer ".concat(this.cookieService.get("JWT"))}}}
    
  public authenticationCheck(){  
    return this.http.get<string>(this.config.API_URL_SERVER,this.getAuthHeader())
    // .pipe(retryWhen(delay(3000)))
    .subscribe(
      () => {this.isAuthenticated = true; this.isOnline = true;},

      (response:HttpErrorResponse) => { 
        
        this.isAuthenticated = false;  
          
        if(response.status==0)
        this.isOnline == false;
        else this.isOnline = true;    
      }
    );
  }
  
      
   public logout(){this.cookieService.delete("JWT"); this.authenticationCheck(); this.redirectIfAuthenticated()}

}
